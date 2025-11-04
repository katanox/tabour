# Tabour

Tabour is a Kotlin library which allows you to produce and consume messages from SQS.

Instead of manually writing your consumers and producers, Tabour allows you to define functions that handle the message
themselves and let Tabour deal with retries, parallelism and error handling.

Currently only the following messaging systems are supported

The library contains the following modules

- [Core](core/README.md)
- [Proto](proto/README.md): Helpers for protobuf serialization and deserialization when consuming and producing to
  queues
- [Spring](spring/README.md): A bean to automatically inject tabour
- Ktor: A Ktor plugin which can register and unregister a tabour instance

To use Tabour **only core** is required**

## Contributing

Please see [CONTRIBUTING](CONTRIBUTING.md) and [CODE_OF_CONDUCT](CODE_OF_CONDUCT.md) for details.

## Security

If you discover any security related issues, please email developers@katanox.com instead of using the issue tracker.

## License

Apache License. Please see [License File](LICENSE) for more information.
