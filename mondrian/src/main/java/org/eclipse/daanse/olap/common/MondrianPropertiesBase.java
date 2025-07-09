/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2001-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * All Rights Reserved.
 *
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 */


package org.eclipse.daanse.olap.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>MondrianProperties</code> contains the properties which determine the
 * behavior of a mondrian instance.
 *
 * <p>There is a method for property valid in a
 * <code>mondrian.properties</code> file. Although it is possible to retrieve
 * properties using the inherited {@link java.util.Properties#getProperty(String)}
 * method, we recommend that you use methods in this class.
 *
 * <h2>Note to developers</h2>
 *
 * If you add a property, you must:<ul>
 *
 * <li>Add a property definition to MondrianProperties.xml.</li>
 *
 * <li>Re-generate MondrianProperties.java using PropertyUtil.</li>
 *
 * <li>Modify the default <code>mondrian.properties</code> file checked into
 * source control, with a description of the property and its default
 * value.</li>
 *
 * <li>Modify the
 * <a target="_top" href="{@docRoot}/../configuration.html#Property_list">
 * Configuration Specification</a>.</li>
 * </ul>
 *
 * <p>Similarly if you update or delete a property.
 *
 * @author jhyde
 * @since 22 December, 2002
 */
public abstract class MondrianPropertiesBase extends Properties {

    private final transient PropertySource propertySource;
    private int populateCount;

    private static final Logger LOGGER =
        LoggerFactory.getLogger(MondrianPropertiesBase.class);

    protected static final String MONDRIAN_DOT_PROPERTIES = "mondrian.properties";

    protected MondrianPropertiesBase(PropertySource propertySource) {
        this.propertySource = propertySource;
    }

    protected Boolean getBoolean(String key, boolean defaultValue) {
        final String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return toBoolean(value);
    }

    private Boolean toBoolean(String value) {
        String trimmedLowerValue = value.toLowerCase().trim();
        return trimmedLowerValue.equals("1")
            || trimmedLowerValue.equals("true")
            || trimmedLowerValue.equals("yes");
    }

    protected Integer getInteger(String key, int defaultValue) {
        final String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    /**
     * Represents a place that properties can be read from, and remembers the
     * timestamp that we last read them.
     */
    public interface PropertySource {
        /**
         * Opens an input stream from the source.
         *
         * <p>Also checks the 'last modified' time, which will determine whether
         * {@link #isStale()} returns true.
         *
         * @return input stream
         */
        InputStream openStream();

        /**
         * Returns true if the source exists and has been modified since last
         * time we called {@link #openStream()}.
         *
         * @return whether source has changed since it was last read
         */
        boolean isStale();

        /**
         * Returns the description of this source, such as a filename or URL.
         *
         * @return description of this PropertySource
         */
        String getDescription();
    }

    /**
     * Implementation of {@link PropertySource} which reads from a
     * {@link java.io.File}.
     */
    static class FilePropertySource implements PropertySource {
        private final File file;
        private long lastModified;

        FilePropertySource(File file) {
            this.file = file;
            this.lastModified = 0;
        }

        @Override
		public InputStream openStream() {
            try {
                this.lastModified = file.lastModified();
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw Util.newInternal(
                    e,
                    new StringBuilder("Error while opening properties file '").append(file).append("'").toString());
            }
        }

        @Override
		public boolean isStale() {
            return file.exists()
                   && file.lastModified() > this.lastModified;
        }

        @Override
		public String getDescription() {
            return new StringBuilder("file=").append(file.getAbsolutePath())
                   .append(" (exists=").append(file.exists()).append(")").toString();
        }
    }

    /**
     * Implementation of {@link PropertySource} which reads from a
     * {@link java.net.URL}.
     */
    static class UrlPropertySource implements PropertySource {
        private final URL url;
        private long lastModified;

        UrlPropertySource(URL url) {
            this.url = url;
        }

        private URLConnection getConnection() {
            try {
                return url.openConnection();
            } catch (IOException e) {
                throw Util.newInternal(
                    e,
                    new StringBuilder("Error while opening properties file '").append(url).append("'").toString());
            }
        }

        @Override
		public InputStream openStream() {
            try {
                final URLConnection connection = getConnection();
                this.lastModified = connection.getLastModified();
                return connection.getInputStream();
            } catch (IOException e) {
                throw Util.newInternal(
                    e,
                    new StringBuilder("Error while opening properties file '").append(url).append("'").toString());
            }
        }

        @Override
		public boolean isStale() {
            final long lastModifiedValue = getConnection().getLastModified();
            return lastModifiedValue > this.lastModified;
        }

        @Override
		public String getDescription() {
            return url.toExternalForm();
        }
    }

    /**
     * Loads this property set from: the file "$PWD/mondrian.properties" (if it
     * exists); the "mondrian.properties" in the CLASSPATH; and from the system
     * properties.
     */
    public void populate() {
        // Read properties file "mondrian.properties", if it exists. If we have
        // read the file before, only read it if it is newer.
        loadIfStale(propertySource);

        URL url = null;
        File file = new File(MONDRIAN_DOT_PROPERTIES);
        if (file.exists() && file.isFile()) {
            // Read properties file "mondrian.properties" from PWD, if it
            // exists.
            try {
                url = file.toURI().toURL();
            } catch (MalformedURLException e) {
                LOGGER.warn(
                    new StringBuilder("Mondrian: file '")
                    .append(file.getAbsolutePath())
                    .append("' could not be loaded").toString(), e);
            }
        } else {
            // Then try load it from classloader
            url =
                MondrianPropertiesBase.class.getClassLoader().getResource(
                    MONDRIAN_DOT_PROPERTIES);
        }

        if (url != null) {
            load(new UrlPropertySource(url));
        } else {
            LOGGER.warn(
                "mondrian.properties can't be found under '{}' or classloader",
                new File(".").getAbsolutePath());
        }

        // copy in all system properties which start with "mondrian."
        int count = 0;
        for (Enumeration<?> keys = System.getProperties().keys();
             keys.hasMoreElements();)
        {
            String key = (String) keys.nextElement();
            String value = System.getProperty(key);
            if (key.startsWith("mondrian.")) {
                // NOTE: the super allows us to bybase calling triggers
                // Is this the correct behavior?
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("populate: key={}, value={}", key, value);
                }
                super.setProperty(key, value);
                count++;
            }
        }
        LOGGER.info(
            "Mondrian: loaded {} system properties", count);
    }

    /**
     * Reads properties from a source.
     * If the source does not exist, or has not changed since we last read it,
     * does nothing.
     *
     * @param source Source of properties
     */
    private void loadIfStale(PropertySource source) {
        if (source.isStale()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Mondrian: loading {}", source.getDescription());
            }
            load(source);
        }
    }

    /**
     * Tries to load properties from a URL. Does not fail, just prints success
     * or failure to log.
     *
     * @param source Source to read properties from
     */
    private void load(final PropertySource source) {
        try(InputStream inputStream = source.openStream()) {
            load(inputStream);
            if (populateCount == 0) {
                LOGGER.info(
                    "Mondrian: properties loaded from '{}'", source.getDescription());
            }
        } catch (IOException e) {
            LOGGER.error(
                "Mondrian: error while loading properties from '{}' ({})", source.getDescription(), e);
        }
    }

    public List<SystemProperty> getPropertyList()
    {
        Field[] fields = getClass().getFields();
        List<SystemProperty> list = new ArrayList /*<Property>*/();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if (field.isAnnotationPresent(PropertyAnnotation.class))
            {
                try {
                    PropertyAnnotation annotation = field.getAnnotation(PropertyAnnotation.class);
                    list.add(new SystemProperty(String.valueOf(field.get(this)), annotation.path()));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(
                        "Error while accessing property '" + field.getName()
                            + "'",
                        e);
                }
            }
        }
        return list;
    }

    public SystemProperty getPropertyDefinition(String path)
    {
        final List<SystemProperty> propertyList = getPropertyList();
        for (int i = 0; i < propertyList.size(); i++) {
            SystemProperty property = (SystemProperty) propertyList.get(i);
            if (property.getPath().equals(path)) {
                return property;
            }
        }
        return null;
    }
}
