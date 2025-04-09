/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package org.eclipse.daanse.lcid;

import java.util.Locale;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

@Component(service = LcidService.class, scope = ServiceScope.SINGLETON)
public class LcidServiceImpl implements LcidService{

    @Override
    public Optional<Locale> lcidToLocale(Optional<Integer> localeIdentifier) {
        if ( localeIdentifier != null && localeIdentifier.isPresent()) {
              LanguageID languageID = LanguageID.lookupByLcid(localeIdentifier.get().shortValue());
              if (languageID != null) {
                 return Optional.of(parseLocale(languageID.getMsId()));
              }
        }
        return Optional.empty();
    }

    /**
     * Parses a locale string.
     *
     * <p>The inverse operation of {@link java.util.Locale#toString()}.
     *
     * @param localeString Locale string, e.g. "en" or "en_US"
     * @return Java locale object
     */
    private Locale parseLocale(String localeString) {
        String[] strings = localeString.split("_");
        switch (strings.length) {
            case 1:
                return Locale.of(strings[0]);
            case 2:
                return Locale.of(strings[0], strings[1]);
            case 3:
                return Locale.of(strings[0], strings[1], strings[2]);
            default:
                throw new RuntimeException(
                    "bad locale string '" + localeString + "'");
        }
    }

}
