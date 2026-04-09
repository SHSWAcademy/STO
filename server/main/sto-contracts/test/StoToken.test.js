const { expect } = require("chai");
const { ehters } = require("hardhat");

describe("StoToken", function () {

    let stoToken;
    let owner;
    let treasury;
    let member1;
    let member2;

    const TOTAL_SUPPLY = 1000000n;
    const HOLDING_SUPPLY = 200000n;  // 플랫폼 구매분

    beforeEach(async function () {
        [owner, treasury, member1, member2] = await ethers.getSigners();

        const StoToken = await ethers.getContractFactory("StoToken");
        stoToken = await StoToken.deploy(
            "강남빌딩토큰",
            "GNB",
            TOTAL_SUPPLY,
            HOLDING_SUPPLY,
            owner.address, // ISSUER 주소
            treasury.address // TREASURY 주소
        );
    });

    it("배포 시 ISSUER가 (총발행량 - 플랫폼구매분)을 받아야 함", async function () {
        const balance = await stoToken.balanceOf(owner.address);
        expect(balance).to.equal(TOTAL_SUPPLY - HOLDING_SUPPLY);
    });

    it("배포 시 TREASURY가 플랫폼 구매분을 받아야 함", async function () {
        const balance = await stoToken.balanceOf(treasury.address);
        expect(balance).to.equal(HOLDING_SUPPLY);
    });

    it("회원이 토큰을 구매하면 ISSUER에서 회원으로 이동해야 함", async function () {
        await stoToken.connect(owner).transfer(member1.address, 1000n);
        expect(await stoToken.balanceOf(member1.address)).to.equal(1000n);
        expect(await stoToken.balanceOf(owner.address)).to.equal(TOTAL_SUPPLY - HOLDING_SUPPLY - 1000n);
    });

    it("잔액 부족 시 transfer 실패해야 함", async function () {
        await expect(
            stoToken.connect(member1).transfer(member2.address, 1n)
        ).to.be.revertedWith("Insufficient balance");
    });

    it("recordTrade는 ISSUER(owner)만 호출 가능해야 함", async function () {
        await expect(
            stoToken.connect(owner).recordTrade(member1.address, member2.address, 100n)
        ).to.not.be.reverted;
    });

    it("일반 회원이 recordTrade 호출 시 실패해야 함", async function () {
        await expect(
            stoToken.connect(member1).recordTrade(member1.address, member2.address, 100n)
        ).to.be.revertedWith("Not owner");
    });

    it("approve 후 transferFrom이 동작해야 함", async function () {
        // ISSUER → member1에게 1000개 전송
        await stoToken.connect(owner).transfer(member1.address, 1000n);
        // member1이 플랫폼(owner)에게 500개 전송 권한 부여
        await stoToken.connect(member1).approve(owner.address, 500n);
        // 플랫폼이 member1 대신 member2에게 500개 전송
        await stoToken.connect(owner).transferFrom(member1.address, member2.address, 500n);

        expect(await stoToken.balanceOf(member1.address)).to.equal(500n);
        expect(await stoToken.balanceOf(member2.address)).to.equal(500n);
    });

});
