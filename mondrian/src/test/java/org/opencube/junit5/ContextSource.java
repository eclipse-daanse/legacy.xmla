/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * History:
 *  This files came from the mondrian project. Some of the Flies
 *  (mostly the Tests) did not have License Header.
 *  But the Project is EPL Header. 2002-2022 Hitachi Vantara.
 *
 * Contributors:
 *   Hitachi Vantara.
 *   SmartCity Jena - initial  Java 8, Junit5
 */
package org.opencube.junit5;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.opencube.junit5.dataloader.DataLoader;
import org.eclipse.daanse.jdbc.datasource.testkit.api.DatabaseProvider;
import org.opencube.junit5.propupdator.TestContextUpdater;

@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ArgumentsSource(ContextArgumentsProvider.class)
@ExtendWith(MondrianRuntimeExtension.class)
public @interface ContextSource {

	Class<? extends DatabaseProvider>[] database() default {};

	Class<? extends DataLoader> dataloader();

	Class<? extends TestContextUpdater>[] propertyUpdater() default {};

}
