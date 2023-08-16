# Tabour

Tabour is a Kotlin library which allows you to interact with message brokers.

[![Build Status](https://github.com/katanox/tabour/workflows/Build/badge.svg)](https://github.com/katanox/tabour)
[![Maven Central](https://img.shields.io/maven-central/v/com.katanox/tabour.svg?label=Maven&logo=apache-maven)](https://search.maven.org/search?q=g:%22com.katanox%22%20AND%20a:%22tabour%22)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)

![Language](https://img.shields.io/badge/Language-Kotlin-blue?style=flat&logo=kotlin)

[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=katanox_tabour&branch=master&metric=bugs)](https://sonarcloud.io/dashboard?id=katanox_tabour)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=katanox_tabour&branch=master&metric=code_smells)](https://sonarcloud.io/dashboard?id=katanox_tabour)
[![Duplicated Lines Density](https://sonarcloud.io/api/project_badges/measure?project=katanox_tabour&branch=master&metric=duplicated_lines_density)](https://sonarcloud.io/dashboard?id=katanox_tabour)
[![Lines of code](https://sonarcloud.io/api/project_badges/measure?project=katanox_tabour&branch=master&metric=ncloc)](https://sonarcloud.io/dashboard?id=katanox_tabour)
[![Maintainability](https://sonarcloud.io/api/project_badges/measure?project=katanox_tabour&branch=master&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=katanox_tabour)
[![Reliability](https://sonarcloud.io/api/project_badges/measure?project=katanox_tabour&branch=master&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=katanox_tabour)
[![Security](https://sonarcloud.io/api/project_badges/measure?project=katanox_tabour&branch=master&metric=security_rating)](https://sonarcloud.io/dashboard?id=katanox_tabour)
[![Technical Dept](https://sonarcloud.io/api/project_badges/measure?project=katanox_tabour&branch=master&metric=sqale_index)](https://sonarcloud.io/dashboard?id=katanox_tabour)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=katanox_tabour&branch=master&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=katanox_tabour)

### Installation

The library contains the following modules

- [Core](core/README.md)
- Proto
- Spring

To use Tabour only **core** is required. Proto and Spring packages contain helpers and functionality regarding 
- Protobuf
- Spring Boot


## Supported messaging systems

- SQS

## Working with self-defined serialization

If you have self-defined methods of serializing objects to string representations (or even want to publish plain-text
messages), you can use the base EventPublisher/EventConsumer classes.

## Contributing

Please see [CONTRIBUTING](CONTRIBUTING.md) and [CODE_OF_CONDUCT](CODE_OF_CONDUCT.md) for details.

## Security

If you discover any security related issues, please email developers@katanox.com instead of using the issue tracker.

## License

Apache License. Please see [License File](LICENSE) for more information.
