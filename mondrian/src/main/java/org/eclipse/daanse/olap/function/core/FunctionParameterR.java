package org.eclipse.daanse.olap.function.core;

import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionParameter;

public record FunctionParameterR(DataType dataType, Optional<String> name, Optional<String> description, Optional<List<String>> reservedWords) implements FunctionParameter{

	public FunctionParameterR(DataType dataType) {
		this(dataType, Optional.empty(), Optional.empty(), Optional.empty());
	}

	public FunctionParameterR(DataType dataType, String name) {
		this(dataType, Optional.ofNullable(name), Optional.empty(), Optional.empty());
	}

	public FunctionParameterR(DataType dataType, String name, String description) {
		this(dataType, Optional.ofNullable(name), Optional.ofNullable(description), Optional.empty());
	}
	
    public FunctionParameterR(DataType dataType, String name, Optional<List<String>> reservedWords) {
        this(dataType, Optional.ofNullable(name), Optional.empty(), reservedWords);
    }

}
