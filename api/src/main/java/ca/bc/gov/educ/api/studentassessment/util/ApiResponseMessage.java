package ca.bc.gov.educ.api.studentassessment.util;

import java.util.Objects;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * ApiResponseMessage
 */
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-02-11T13:04:32.545-08:00")

public class ApiResponseMessage   {
  @JsonProperty("message")
  private String message = null;

  @JsonProperty("reference")
  private String reference = null;

  
  public ApiResponseMessage(String message, String reference, MessageTypeEnum messageType) {
	super();
	this.message = message;
	this.reference = reference;
	this.messageType = messageType;
}
  
  public ApiResponseMessage(String message, MessageTypeEnum messageType) {
	super();
	this.message = message;
	this.messageType = messageType;
}

  /**
   * Gets or Sets messageType
   */
  public enum MessageTypeEnum {
    ERROR("error"),
    
    WARNING("warning");

    private String value;

    MessageTypeEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static MessageTypeEnum fromValue(String text) {
      for (MessageTypeEnum b : MessageTypeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("messageType")
  private MessageTypeEnum messageType = null;

  public ApiResponseMessage message(String message) {
    this.message = message;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public ApiResponseMessage reference(String reference) {
    this.reference = reference;
    return this;
  }

  public String getReference() {
    return reference;
  }

  public void setReference(String reference) {
    this.reference = reference;
  }

  public ApiResponseMessage messageType(MessageTypeEnum messageType) {
    this.messageType = messageType;
    return this;
  }

  public MessageTypeEnum getMessageType() {
    return messageType;
  }

  public void setMessageType(MessageTypeEnum messageType) {
    this.messageType = messageType;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiResponseMessage apiResponseMessage = (ApiResponseMessage) o;
    return Objects.equals(this.message, apiResponseMessage.message) &&
        Objects.equals(this.reference, apiResponseMessage.reference) &&
        Objects.equals(this.messageType, apiResponseMessage.messageType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(message, reference, messageType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiResponseMessage {\n");
    
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    reference: ").append(toIndentedString(reference)).append("\n");
    sb.append("    messageType: ").append(toIndentedString(messageType)).append("\n");
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

