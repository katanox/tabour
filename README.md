# Tabour

Tabour is a Kotlin library which allows you to produce and consume messages from with messaging systems.

Instead of manually writing your consumers and producers, Tabour allows you to define functions that handle the message themselves and let Tabour deal with retries, parallelism and error handling.

Currently only the following messaging systems are supported

1. SQS

The library contains the following modules

- [Core](core/README.md)
- [Proto](proto/README.md)
- [Spring](spring/README.md)

To use Tabour only **core** is required. Proto and Spring packages contain helpers and functionality regarding

- Protobuf
- Spring Boot


## Contributing

Please see [CONTRIBUTING](CONTRIBUTING.md) and [CODE_OF_CONDUCT](CODE_OF_CONDUCT.md) for details.

## Security

If you discover any security related issues, please email developers@katanox.com instead of using the issue tracker.

## License

Apache License. Please see [License File](LICENSE) for more information.
