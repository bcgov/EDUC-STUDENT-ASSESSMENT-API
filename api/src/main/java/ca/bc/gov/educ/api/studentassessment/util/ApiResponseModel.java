package ca.bc.gov.educ.api.studentassessment.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import ca.bc.gov.educ.api.studentassessment.util.ApiResponseMessage.MessageTypeEnum;




public class ApiResponseModel<T> {

	public ApiResponseModel(CodeEnum code, T value) {
		this.code = code;
		this.value = value;
	}

	public static <T> ApiResponseModel<T> SUCCESS(T value) {
		ApiResponseModel<T> response = new ApiResponseModel<T>(CodeEnum.SUCCESS, value);
		return response;
	}

	public static <T> ApiResponseModel<T> ERROR(T value) {
		ApiResponseModel<T> response = new ApiResponseModel<T>(CodeEnum.ERROR, value);
		return response;
	}

	public static <T> ApiResponseModel<T> ERROR(T value, List<String> errorMessages) {
		ApiResponseModel<T> response = new ApiResponseModel<T>(CodeEnum.ERROR, value);
		for (String s : errorMessages) {
			response.addMessageItem(s, MessageTypeEnum.ERROR);
		}
		return response;
	}

	public static <T> ApiResponseModel<T> ERROR(T value, String... errorMessages) {
		ApiResponseModel<T> response = new ApiResponseModel<T>(CodeEnum.ERROR, value);
		for (String s : errorMessages) {
			response.addMessageItem(s, MessageTypeEnum.ERROR);
		}
		return response;
	}

	public void addErrorMessages(List<String> errorMessages) {
		for (String s : errorMessages) {
			addMessageItem(s, MessageTypeEnum.ERROR);
		}
		code = CodeEnum.ERROR;
		
	}

	public void addWarningMessages(List<String> warningMessages) {
		for (String s : warningMessages) {
			addMessageItem(s, MessageTypeEnum.WARNING);
		}
		if (code != CodeEnum.ERROR ) {
			code = CodeEnum.WARNING;
		}
	}

	
	public static <T> ApiResponseModel<T> WARNING(T value) {
		ApiResponseModel<T> response = new ApiResponseModel<T>(CodeEnum.WARNING, value);
		return response;
	}

	public static <T> ApiResponseModel<T> WARNING(T value, List<String> warningMessages) {
		ApiResponseModel<T> response = new ApiResponseModel<T>(CodeEnum.WARNING, value);
		for (String s : warningMessages) {
			response.addMessageItem(s, MessageTypeEnum.WARNING);
		}
		return response;
	}

	public enum CodeEnum {
		SUCCESS("success"),

		ERROR("error"),

		WARNING("warning");

		private String value;

		CodeEnum(String value) {
			this.value = value;
		}

		@Override
		@JsonValue
		public String toString() {
			return String.valueOf(value);
		}

		@JsonCreator
		public static CodeEnum fromValue(String text) {
			for (CodeEnum b : CodeEnum.values()) {
				if (String.valueOf(b.value).equals(text)) {
					return b;
				}
			}
			return null;
		}
	}

	@JsonProperty("code")
	private CodeEnum code = null;

	@JsonProperty("value")
	private T value = null;

	@JsonProperty("messages")
	@Valid
	private List<ApiResponseMessage> messages = new ArrayList<ApiResponseMessage>();;

	public CodeEnum getCode() {
		return code;
	}


	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}



	public void addMessageItem(String message, MessageTypeEnum status) {
		ApiResponseMessage responseMessage = new ApiResponseMessage(message, status);
		messages.add(responseMessage);
	}

	@Valid
	public List<ApiResponseMessage> getMessages() {
		return messages;
	}


	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(java.lang.Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ApiResponseModel<T> _apiResponse = (ApiResponseModel<T>) o;
		return Objects.equals(this.code, _apiResponse.code) && Objects.equals(this.value, _apiResponse.value)
				&& Objects.equals(this.messages, _apiResponse.messages);
	}

	@Override
	public int hashCode() {
		return Objects.hash(code, value, messages);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class ModelApiResponse {\n");

		sb.append("    code: ").append(toIndentedString(code)).append("\n");
		sb.append("    value: ").append(toIndentedString(value)).append("\n");
		sb.append("    message: ").append(toIndentedString(messages)).append("\n");
		sb.append("}");
		return sb.toString();
	}

	/**
	 * Convert the given object to string with each line indented by 4 spaces
	 * (except the first line).
	 */
	private String toIndentedString(java.lang.Object o) {
		if (o == null) {
			return "null";
		}
		return o.toString().replace("\n", "\n    ");
	}
}
