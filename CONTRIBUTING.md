# Contributing

Thanks for helping! A few things to know first.

**This repository is a published mirror.** The SDKs are developed in the txApi main repository
together with the API, because an API change and the matching change in all four SDKs go in the
same commit (the coverage tests fail otherwise). Every release replaces the contents here.

That means:

- **Issues** are the best way to report a bug, a missing feature or unclear documentation.
  Please name the SDK, its version, and — for bugs — a minimal example and what you expected.
- **Pull requests** are welcome. We review them here, apply the change in the main repository
  and credit you in the commit; your PR is closed once the next release contains it. Please keep
  a PR to one SDK and one topic, and run that SDK's tests (see README → Development).
- **New endpoints** cannot be added from here: `spec/endpoints.json` and the generated code
  (`python/src/txapi/_generated.py`, `fivem/lib/endpoints.lua`) come from the API catalog. Open
  an issue instead.
- **Behaviour** that all SDKs share is defined in [SPEC.md](SPEC.md). A change there has to be
  made in all four SDKs at once.

By contributing you agree that your contribution is licensed under the MIT license of this
repository.
