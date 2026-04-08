require("@nomicfoundation/hardhat-toolbox");
require("dotenv").config();

/** @type import('hardhat/config').HardhatUserConfig */
module.exports = {
  solidity: "0.8.28",
  networks: {
    // 로컬테스트용
    localhost: {
      url: "http://127.0.0.1:8545"
    },
    // 실제 테스트넷
    sepolia: {
      url: process.env.SEPOLIA_RPC_URL,
      accounts: [process.env.ISSUER_PRIVATE_KEY]
    }
  }
};
