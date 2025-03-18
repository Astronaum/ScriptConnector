package ScriptConnector;

import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.filter.AbstractFilterTranslator;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;

public class ScriptFilter extends AbstractFilterTranslator<String> {

    @Override
    protected String createEqualsExpression(EqualsFilter filter, boolean not) {
        if (filter.getAttribute().is(Name.NAME)) {
            return filter.getAttribute().getValue().toString();
        }
        return null; // Unsupported filter
    }
}