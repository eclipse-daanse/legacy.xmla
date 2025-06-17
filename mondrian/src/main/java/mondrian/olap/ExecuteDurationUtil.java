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
package mondrian.olap;

import java.time.Duration;
import java.util.Optional;

import org.eclipse.daanse.olap.api.ConfigConstants;
import org.eclipse.daanse.olap.api.Context;

public class ExecuteDurationUtil {

    public static Optional<Duration> executeDurationValue(Context<?> context) {
        long duration = context.getConfigValue(ConfigConstants.EXECUTE_DURATION, ConfigConstants.EXECUTE_DURATION_DEFAULT_VALUE, Long.class);
        if (duration > 0) {
            String unit = context.getConfigValue(ConfigConstants.EXECUTE_DURATION_UNIT, ConfigConstants.EXECUTE_DURATION_UNIT_DEFAULT_VALUE, String.class).toLowerCase();
            switch (unit) {
                case "milliseconds":
                    return Optional.of(Duration.ofMillis(duration));
                case "seconds":
                    return Optional.of(Duration.ofSeconds(duration));
                default:
                    return Optional.of(Duration.ofMillis(duration));
            }
        }
        return Optional.empty();
    }

}
