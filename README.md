# Blockchain Mobile Wallet

A robust Android mobile application that interfaces with blockchain smart contracts, providing seamless cryptocurrency management with fiat currency integration.

## 🌟 Features

- **Multi-Currency Support**: Select your preferred currency (EUR/USD)
- **Currency Exchange**: Seamlessly convert between EUR and USD
- **Cross-Border Transfers**: Send funds to other accounts with on-chain exchange (similar to SWIFT)
- **Multi-Wallet Management**: Add and switch between multiple wallets
- **Smart Contract Integration**: Direct interaction with blockchain via node connection
- **Transaction Support**: Both view functions (read-only) and state-modifying transactions

## 🛠️ Technical Implementation

This application demonstrates advanced blockchain integration on Android using:

- **Optimized Threading**: Improved thread management for responsive UI during blockchain operations
- **Direct Smart Contract Calls**: Interacts with smart contracts without wrapper generation
- **Asynchronous Processing**: Non-blocking operations for better user experience

## 🔧 Dependencies

The project leverages the Web3j ecosystem along with:

- **Web3j**: Core library for Ethereum/EVM-compatible blockchain interaction
- **OkHttp3**: Efficient HTTP client for network communication with the blockchain node
- **RxJava**: Reactive programming for managing asynchronous blockchain calls
- **Jackson**: Fast JSON serialization/deserialization for blockchain data handling
- **SLF4J**: Logging framework for Web3j operations
- **Web3j ABI**: Direct smart contract interaction via ABI and address

## 🚀 Getting Started

1. Clone the repository
2. Open in Android Studio
4. Run on an emulator or physical device

## 🔗 Smart Contract Integration

The application connects to a local blockchain node to:
- Read contract state (view functions)
- Submit transactions (state-modifying functions)
- Monitor events from smart contracts

## 🛣️ Roadmap

- [ ] Add support for additional cryptocurrencies
- [ ] Implement transaction history and analytics
- [ ] Enhanced security features (biometric authentication)
- [ ] DApp browser integration
