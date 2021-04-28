package ca.bc.gov.educ.api.studentassessment.util;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import ca.bc.gov.educ.api.studentassessment.exception.GradBusinessRuleException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

@Service
@Scope(proxyMode = ScopedProxyMode.DEFAULT)
public class GradValidation {

	private static final ThreadLocal<List<String>> warningList = ThreadLocal.<List<String>>withInitial(() -> {return new LinkedList<String>();});
	private static final ThreadLocal<List<String>> errorList = ThreadLocal.<List<String>>withInitial(() -> {return new LinkedList<String>();});

	@Autowired
	MessageHelper messagesHelper;
	
	public void addWarning(String warningMessage) {
		warningList.get().add(warningMessage);
	}

	public void addWarning(String formattedWarningMessage, Object... args) {
		warningList.get().add(String.format(formattedWarningMessage, args));
	}

	public void addError(String errorMessage) {
		errorList.get().add(errorMessage);
	}

	public void addError(String formattedErrorMessage, Object... args) {
		errorList.get().add(String.format(formattedErrorMessage, args));
	}

	public void addErrorAndStop(String errorMessage) {
		errorList.get().add(errorMessage);
		throw new GradBusinessRuleException();

	}

	public void addErrorAndStop(String formattedErrorMessage, Object... args) {
		errorList.get().add(String.format(formattedErrorMessage, args));
		throw new GradBusinessRuleException();

	}

	
	public List<String> getWarnings() {
		return warningList.get();
	}
	
	public List<String> getErrors() {
		return errorList.get();
	}
	
    public void ifErrors(Consumer<List<String>> action) {
        if (!errorList.get().isEmpty()) {
            action.accept(errorList.get());
        }
    }

    public void ifWarnings(Consumer<List<String>> action) {
        if (!warningList.get().isEmpty()) {
            action.accept(warningList.get());
        }
    }
    
    public boolean requiredField(Object requiredValue, String fieldName) {
    	if (requiredValue == null) {
    		addError(messagesHelper.missingValue(fieldName));
    		return false;
    	}
    	if (requiredValue instanceof String) {
        	if (StringUtils.isBlank((String)requiredValue)) {
        		addError(messagesHelper.missingValue(fieldName));
        		return false;
        	}
    	}
    	return true;
    }
    
    public void stopOnErrors() {
    	if (hasErrors()) {
    		throw new GradBusinessRuleException();
    	}
    }
    
    
    
    public boolean hasErrors() {
    	return !errorList.get().isEmpty();
    }
    
    public boolean hasWarnings() {
    	return !warningList.get().isEmpty();
    }
    
    public void clear() {
    	errorList.get().clear();
    	warningList.get().clear();
    	
    }
}