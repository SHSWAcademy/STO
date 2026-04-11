const fs = require("fs");
const path = require("path");

const artifactPath = path.join(__dirname, "artifacts/contracts/StoToken.sol/StoToken.json");
const outputDir = path.join(__dirname, "generated");

const artifact = JSON.parse(fs.readFileSync(artifactPath, "utf8"));

if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true });
}

fs.writeFileSync(
    path.join(outputDir, "StoToken.abi"),
    JSON.stringify(artifact.abi, null, 2),
    "utf8"
);

fs.writeFileSync(
    path.join(outputDir, "StoToken.bin"),
    artifact.bytecode.startsWith("0x") ? artifact.bytecode.slice(2) : artifact.bytecode,
    "utf8"
);

console.log("ABI/BIN 생성 완료");
console.log(path.join(outputDir, "StoToken.abi"));
console.log(path.join(outputDir, "StoToken.bin"));