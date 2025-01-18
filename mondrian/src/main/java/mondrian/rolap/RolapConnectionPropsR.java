package mondrian.rolap;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.eclipse.daanse.olap.api.ConnectionProps;

public record RolapConnectionPropsR(List<String> roles, boolean useSchemaPool, Locale locale, Duration pinSchemaTimeout,
		 Optional<String> aggregateScanSchema, Optional<String> aggregateScanCatalog)
		implements ConnectionProps {

	private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(-1);

	public RolapConnectionPropsR() {
		this(List.of(), true, Locale.getDefault(), DEFAULT_TIMEOUT, Optional.empty(), Optional.empty());
	}

    public RolapConnectionPropsR(List<String> roles) {
        this(roles, true, Locale.getDefault(), DEFAULT_TIMEOUT, Optional.empty(), Optional.empty());
    }
}
