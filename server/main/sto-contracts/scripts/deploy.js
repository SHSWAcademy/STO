const { ethers } = require("hardhat");

async function main() {
    const [deployer] = await ethers.getSigners();
    console.log("배포 계정 (ISSUER):", deployer.address);

    const balance = await ethers.provider.getBalance(deployer.address);
    console.log("잔액:", ethers.formatEther(balance), "ETH");

    const StoToken = await ethers.getContractFactory("StoToken");

    const tokenName = "STO Test Token";
    const tokenSymbol = "STOT";
    const totalSupply = 1000000n;
    const holdingSupply = 200000n;
    const issuerAddress = deployer.address;
    const treasuryAddress = "0xdf67c05fcbb0196b6913c649931f2930d140b610";

    console.log("컨트랙트 배포 중...");

   const token = await StoToken.deploy(
       tokenName,
       tokenSymbol,
       totalSupply,
       holdingSupply,
       issuerAddress,
       treasuryAddress
   );

   await token.waitForDeployment();

   const contractAddress = await token.getAddress();
   console.log("컨트랙트 배포 완료: ", contractAddress);
}

main().catch((error) => {
    console.error(error);
    process.exit(1);
});