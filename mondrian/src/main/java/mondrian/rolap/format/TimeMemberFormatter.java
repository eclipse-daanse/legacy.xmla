package mondrian.rolap.format;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.formatter.MemberFormatter;

public class TimeMemberFormatter implements MemberFormatter {

    private final DateTimeFormatter inFormatter;
    private final DateTimeFormatter outFormatter;
    private final ZoneId zone;

    public TimeMemberFormatter() {

        inFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss.S");
        outFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

        zone = ZoneId.of("Europe/Berlin");
    }

    public String format(Member member) {

        String value = member.getName();

        LocalDateTime local = LocalDateTime.parse(value, inFormatter);
        ZonedDateTime zoned = local.atZone(zone);
        return zoned.format(outFormatter);

    }

}
