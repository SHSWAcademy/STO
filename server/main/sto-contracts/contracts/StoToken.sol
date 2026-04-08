// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract StoToken {

    // 기본 정보
    string public name;
    string public symbol;
    uint8 public decimals = 0; // 부동산 토큰은 소수점 없음
    uint256 public totalSupply;

    address public owner; // ISSUER
    address public treasury; // PLATFORM_TREASURY

    // ERC-20 상태
    mapping(address => uint256) public balanceOf;
    mapping(address => mapping(address => uint256)) public allowance;

    // 이벤트
    event Transfer(address indexed from, address indexed to, uint256 value);
    event Approval(address indexed owner, address indexed spender, uint256 value);
    event TradeRecorded(address indexed seller, address indexed buyer, uint256 amount, uint256 timestamp);

    modifier onlyOwner() {
        require(msg.sender == owner, "Not owner");
        _;
    }

    // 배포 시 실행
    constructor(
        string memory _name,
        string memory _symbol,
        uint256 _totalSupply,
        uint256 _holdingSupply, // 플랫폼 구매토큰
        address _issuer, //회원 판매용 토큰 보유 주소
        address _treasury // 플랫폼 구매분 보유 주소
    ) {
        name = _name;
        symbol = _symbol;
        totalSupply = _totalSupply;
        owner = msg.sender; // ISSUER가 owner
        treasury = _treasury;

        // ISSUER: 총 발행량 - 플랫폼 구매분
        balanceOf[_issuer] = _totalSupply - _holdingSupply;

        // TREASURY: 플랫폼 구매분
        balanceOf[_treasury] = _holdingSupply;

        emit Transfer(address(0), _issuer, _totalSupply - _holdingSupply);
        emit Transfer(address(0), _treasury, _holdingSupply);
    }

    // ERC-20 표준 함수
    function transfer(address to, uint256 amount) external returns (bool) {
        require(balanceOf[msg.sender] >= amount, "Insufficient balance");
        balanceOf[msg.sender] -= amount;
        balanceOf[to] += amount;
        emit Transfer(msg.sender, to, amount);
        return true;
    }

    function approve(address spender, uint256 amount) external returns (bool) {
        allowance[msg.sender][spender] = amount;
        emit Approval(msg.sender, spender, amount);
        return true;
    }

    function transferFrom(address from, address to, uint256 amount) external returns (bool) {
        require(balanceOf[from] >= amount, "Insufficient balance");
        require(allowance[from][msg.sender] >= amount, "Insufficient allowance");
        allowance[from][msg.sender] -= amount;
        balanceOf[from] -= amount;
        balanceOf[to] += amount;
        emit Transfer(from, to, amount);
        return true;
    }

    // STO 전용: 거래 온체인 기록
    function recordTrade(
        address seller,
        address buyer,
        uint256 amount
    ) external onlyOwner {
        emit TradeRecorded(seller, buyer, amount, block.timestamp);
    }

}