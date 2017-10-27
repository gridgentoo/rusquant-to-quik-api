package ru.rusquant.messages.response.body;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 *   Base class for response body.
 *   Request body contains the parameters of request.
 *   Author: Aleksey Kutergin <aleksey.v.kutergin@gmail.ru>
 *   Company: Rusquant
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
		@JsonSubTypes.Type(value = EchoResponseBody.class, 				name = "EchoResponseBody"),
		@JsonSubTypes.Type(value = ConnectionSateResponseBody.class, 	name = "ConnectionSateResponseBody"),
		@JsonSubTypes.Type(value = InfoParameterResponseBody.class, 	name = "InfoParameterResponseBody"),
		@JsonSubTypes.Type(value = TransactionResponseBody.class, 		name = "TransactionResponseBody"),
		@JsonSubTypes.Type(value = OrderResponseBody.class, 			name = "OrderResponseBody"),
		@JsonSubTypes.Type(value = TradesResponseBody.class, 			name = "TradesResponseBody")
})
public abstract class ResponseBody
{

}
