# APIM Adapter
A low latency API Management Adapter for Java Applications.

See also: https://confluence.sbb.ch/pages/viewpage.action?pageId=915381059

## Big Pucture
![HowItWorks](HowItWorks.jpg)

## Features
- AuthRep for all requests (except of the first request for each client after restart) is done asynchronously: means, the calculations are based on the state of the cache.
- Average latency for AuthRep: 0.3ms
- Synchronization with 3Scale backend is done periodically, single-threaded and in a configurable interval >= 1s
- Average latency for Sync with 3Scale Backend: 0.2ms
- Synchronization with 3Scale backend includes the following: report hit-count per metric if it is greater than 0 (and reset local counter back to 0), load consistent usages and use them as the new base values for counting. Reporting is only done when there are Hits to pe reported. Synchronization of the Usages are done every time.
## Improvements
- Preload initial configs from 3Scale already at startup to reduce latency of first request
- Merge MappingRules for the plan of a client at sync
- Introduce cirquit breaker for all dependencies
## Further roadmap
- Automatic Testing
- Periodically persist caches locally in order to be more resilient
- Introduce a shared memory cache to be more consistent

## Performance Reports
coming soon
