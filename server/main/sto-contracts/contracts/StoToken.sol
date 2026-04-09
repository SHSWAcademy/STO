// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

contract StoToken is ERC20, Ownable {

    address public treasury;

    event TradeRecorded(
        uint256 indexed tradeId,
        address indexed seller,
        address indexed buyer,
        uint256 quantity,
        uint256 price
    );

    constructor(
        string memory _name,
        string memory _symbol,
        uint256 _totalSupply,
        uint256 _holdingSupply,
        address _issuer,
        address _treasury
    ) ERC20(_name, _symbol) Ownable(_issuer) {

        require(_holdingSupply <= _totalSupply, "holding exceeds total");

        treasury = _treasury;
        _mint(_issuer, _totalSupply - _holdingSupply);
        _mint(_treasury, _holdingSupply);
    }

    // 부동산 토큰은 소수점 없음
    function decimals() public pure override returns (uint8) {
        return 0;
    }

    // 거래 온체인 기록 (owner = ISSUER만 호출 가능)
    function recordTrade(
        uint256 tradeId,
        address seller,
        address buyer,
        uint256 quantity,
        uint256 price
    ) external onlyOwner {
        emit TradeRecorded(tradeId, seller, buyer, quantity, price);
    }
}