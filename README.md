Simple mobile application developed in Android to interact with a local chain deployed smart contract by connecting to the node and calling a view type function that doesn't modify the state of the contract (no transaction sent). Feature improvments: better management of threads with other methods + adding transaction functionality.
This project uses web3j library with it's runtime dependencies:
- okhttp3 for network communication with the chain
- rxjava for asynchronous function calls
- jackson for serialization/deserialization of objects
- slf4j for web3j logging
- web3j abi for interacting with the contracts by abi and address, avoiding creating wrappers over the contracts
