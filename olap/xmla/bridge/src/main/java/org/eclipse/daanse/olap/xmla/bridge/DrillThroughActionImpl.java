/*
* Copyright (c) 2024 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   SmartCity Jena - initial
*   Stefan Bischof (bipolis.org) - initial
*/
package org.eclipse.daanse.olap.xmla.bridge;

import org.eclipse.daanse.olap.action.api.DrillThroughAction;
import org.eclipse.daanse.xmla.api.common.enums.ActionTypeEnum;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;

import static org.eclipse.daanse.olap.xmla.bridge.DrillThroughUtils.getCoordinateElements;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component(service = DrillThroughAction.class)
@Designate(factory = true, ocd = DrillThroughActionConfig.class)
public class DrillThroughActionImpl extends AbstractAction implements DrillThroughAction {

    private static final Converter CONVERTER = Converters.standardConverter();
    private DrillThroughActionConfig config;

    @Activate
    void activate(Map<String, Object> props) {
        this.config = CONVERTER.convert(props).to(DrillThroughActionConfig.class);
    }

    @Override
    public String content(String coordinate, String cubeName) {
        List<String> coordinateElements = getCoordinateElements(coordinate);
        return DrillThroughUtils.getDrillThroughQueryByColumns(coordinateElements, catalogs().orElse(List.of()), cubeName);
    }

    @Override
    protected AbstractActionConfig getConfig() {
        return config;
    }

    @Override
    public Optional<List<String>> catalogs() {
        return Optional.ofNullable(config.columns());
    }
}
