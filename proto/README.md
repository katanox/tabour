# Proto

The proto module contains functions that you can use to serialize and deserialize queue messages.

It provides the following functions:

1. `fromSqsMessage`

    This function deserializes a SQS message without handling any deserialization errors. If the deserialization throws an exception, you  are responsible to handle it. 

    You can use this method if you are 100% sure that the message is correct and there is no chance of it failing
2. `fromSqsMessageOrNull` in different flavours that handle the errors. 

    Depending on the flavour, you can :
     1. map the successful deserialized value
     2. handle the error
3. `ProtobufMessage.jsonify()` which converts a protobuf message to its json representation