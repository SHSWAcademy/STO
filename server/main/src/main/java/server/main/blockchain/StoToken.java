package server.main.blockchain;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.CustomError;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/LFDT-web3j/web3j/tree/main/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 1.8.0.
 */
@SuppressWarnings("rawtypes")
@Generated("org.web3j.codegen.SolidityFunctionWrapperGenerator")
public class StoToken extends Contract {
    public static final String BINARY = "608060405234801561001057600080fd5b50604051611ee4380380611ee48339818101604052810190610032919061070f565b818686816003908161004491906109eb565b50806004908161005491906109eb565b505050600073ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff16036100c95760006040517f1e4fbdf70000000000000000000000000000000000000000000000000000000081526004016100c09190610acc565b60405180910390fd5b6100d88161019360201b60201c565b508383111561011c576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161011390610b44565b60405180910390fd5b80600660006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555061017882848661016d9190610b93565b61025960201b60201c565b610188818461025960201b60201c565b505050505050610c5c565b6000600560009054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905081600560006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055508173ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a35050565b600073ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff16036102cb5760006040517fec442f050000000000000000000000000000000000000000000000000000000081526004016102c29190610acc565b60405180910390fd5b6102dd600083836102e160201b60201c565b5050565b600073ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff16036103335780600260008282546103279190610bc7565b92505081905550610406565b60008060008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020549050818110156103bf578381836040517fe450d38c0000000000000000000000000000000000000000000000000000000081526004016103b693929190610c0a565b60405180910390fd5b8181036000808673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002081905550505b600073ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff160361044f578060026000828254039250508190555061049c565b806000808473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020600082825401925050819055505b8173ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef836040516104f99190610c41565b60405180910390a3505050565b6000604051905090565b600080fd5b600080fd5b600080fd5b600080fd5b6000601f19601f8301169050919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052604160045260246000fd5b61056d82610524565b810181811067ffffffffffffffff8211171561058c5761058b610535565b5b80604052505050565b600061059f610506565b90506105ab8282610564565b919050565b600067ffffffffffffffff8211156105cb576105ca610535565b5b6105d482610524565b9050602081019050919050565b60005b838110156105ff5780820151818401526020810190506105e4565b60008484015250505050565b600061061e610619846105b0565b610595565b90508281526020810184848401111561063a5761063961051f565b5b6106458482856105e1565b509392505050565b600082601f8301126106625761066161051a565b5b815161067284826020860161060b565b91505092915050565b6000819050919050565b61068e8161067b565b811461069957600080fd5b50565b6000815190506106ab81610685565b92915050565b600073ffffffffffffffffffffffffffffffffffffffff82169050919050565b60006106dc826106b1565b9050919050565b6106ec816106d1565b81146106f757600080fd5b50565b600081519050610709816106e3565b92915050565b60008060008060008060c0878903121561072c5761072b610510565b5b600087015167ffffffffffffffff81111561074a57610749610515565b5b61075689828a0161064d565b965050602087015167ffffffffffffffff81111561077757610776610515565b5b61078389828a0161064d565b955050604061079489828a0161069c565b94505060606107a589828a0161069c565b93505060806107b689828a016106fa565b92505060a06107c789828a016106fa565b9150509295509295509295565b600081519050919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052602260045260246000fd5b6000600282049050600182168061082657607f821691505b602082108103610839576108386107df565b5b50919050565b60008190508160005260206000209050919050565b60006020601f8301049050919050565b600082821b905092915050565b6000600883026108a17fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff82610864565b6108ab8683610864565b95508019841693508086168417925050509392505050565b6000819050919050565b60006108e86108e36108de8461067b565b6108c3565b61067b565b9050919050565b6000819050919050565b610902836108cd565b61091661090e826108ef565b848454610871565b825550505050565b600090565b61092b61091e565b6109368184846108f9565b505050565b5b8181101561095a5761094f600082610923565b60018101905061093c565b5050565b601f82111561099f576109708161083f565b61097984610854565b81016020851015610988578190505b61099c61099485610854565b83018261093b565b50505b505050565b600082821c905092915050565b60006109c2600019846008026109a4565b1980831691505092915050565b60006109db83836109b1565b9150826002028217905092915050565b6109f4826107d4565b67ffffffffffffffff811115610a0d57610a0c610535565b5b610a17825461080e565b610a2282828561095e565b600060209050601f831160018114610a555760008415610a43578287015190505b610a4d85826109cf565b865550610ab5565b601f198416610a638661083f565b60005b82811015610a8b57848901518255600182019150602085019450602081019050610a66565b86831015610aa85784890151610aa4601f8916826109b1565b8355505b6001600288020188555050505b505050505050565b610ac6816106d1565b82525050565b6000602082019050610ae16000830184610abd565b92915050565b600082825260208201905092915050565b7f686f6c64696e67206578636565647320746f74616c0000000000000000000000600082015250565b6000610b2e601583610ae7565b9150610b3982610af8565b602082019050919050565b60006020820190508181036000830152610b5d81610b21565b9050919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052601160045260246000fd5b6000610b9e8261067b565b9150610ba98361067b565b9250828203905081811115610bc157610bc0610b64565b5b92915050565b6000610bd28261067b565b9150610bdd8361067b565b9250828201905080821115610bf557610bf4610b64565b5b92915050565b610c048161067b565b82525050565b6000606082019050610c1f6000830186610abd565b610c2c6020830185610bfb565b610c396040830184610bfb565b949350505050565b6000602082019050610c566000830184610bfb565b92915050565b61127980610c6b6000396000f3fe608060405234801561001057600080fd5b50600436106100ea5760003560e01c8063715018a61161008c57806395d89b411161006657806395d89b411461023b578063a9059cbb14610259578063dd62ed3e14610289578063f2fde38b146102b9576100ea565b8063715018a6146101f75780637328d569146102015780638da5cb5b1461021d576100ea565b806323b872dd116100c857806323b872dd1461015b578063313ce5671461018b57806361d027b3146101a957806370a08231146101c7576100ea565b806306fdde03146100ef578063095ea7b31461010d57806318160ddd1461013d575b600080fd5b6100f76102d5565b6040516101049190610e29565b60405180910390f35b61012760048036038101906101229190610ee4565b610367565b6040516101349190610f3f565b60405180910390f35b61014561038a565b6040516101529190610f69565b60405180910390f35b61017560048036038101906101709190610f84565b610394565b6040516101829190610f3f565b60405180910390f35b6101936103c3565b6040516101a09190610ff3565b60405180910390f35b6101b16103c8565b6040516101be919061101d565b60405180910390f35b6101e160048036038101906101dc9190611038565b6103ee565b6040516101ee9190610f69565b60405180910390f35b6101ff610436565b005b61021b60048036038101906102169190611065565b61044a565b005b6102256104c1565b604051610232919061101d565b60405180910390f35b6102436104eb565b6040516102509190610e29565b60405180910390f35b610273600480360381019061026e9190610ee4565b61057d565b6040516102809190610f3f565b60405180910390f35b6102a3600480360381019061029e91906110e0565b6105a0565b6040516102b09190610f69565b60405180910390f35b6102d360048036038101906102ce9190611038565b610627565b005b6060600380546102e49061114f565b80601f01602080910402602001604051908101604052809291908181526020018280546103109061114f565b801561035d5780601f106103325761010080835404028352916020019161035d565b820191906000526020600020905b81548152906001019060200180831161034057829003601f168201915b5050505050905090565b6000806103726106ad565b905061037f8185856106b5565b600191505092915050565b6000600254905090565b60008061039f6106ad565b90506103ac8582856106c7565b6103b785858561075c565b60019150509392505050565b600090565b600660009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b60008060008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020549050919050565b61043e610850565b61044860006108d7565b565b610452610850565b8273ffffffffffffffffffffffffffffffffffffffff168473ffffffffffffffffffffffffffffffffffffffff16867f753ae2aaaf39e14f83785986367e2865dbbccf8c9b24ce246f61939bc0f5d7a885856040516104b2929190611180565b60405180910390a45050505050565b6000600560009054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905090565b6060600480546104fa9061114f565b80601f01602080910402602001604051908101604052809291908181526020018280546105269061114f565b80156105735780601f1061054857610100808354040283529160200191610573565b820191906000526020600020905b81548152906001019060200180831161055657829003601f168201915b5050505050905090565b6000806105886106ad565b905061059581858561075c565b600191505092915050565b6000600160008473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002054905092915050565b61062f610850565b600073ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff16036106a15760006040517f1e4fbdf7000000000000000000000000000000000000000000000000000000008152600401610698919061101d565b60405180910390fd5b6106aa816108d7565b50565b600033905090565b6106c2838383600161099d565b505050565b60006106d384846105a0565b90507fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff8110156107565781811015610746578281836040517ffb8f41b200000000000000000000000000000000000000000000000000000000815260040161073d939291906111a9565b60405180910390fd5b6107558484848403600061099d565b5b50505050565b600073ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff16036107ce5760006040517f96c6fd1e0000000000000000000000000000000000000000000000000000000081526004016107c5919061101d565b60405180910390fd5b600073ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff16036108405760006040517fec442f05000000000000000000000000000000000000000000000000000000008152600401610837919061101d565b60405180910390fd5b61084b838383610b74565b505050565b6108586106ad565b73ffffffffffffffffffffffffffffffffffffffff166108766104c1565b73ffffffffffffffffffffffffffffffffffffffff16146108d5576108996106ad565b6040517f118cdaa70000000000000000000000000000000000000000000000000000000081526004016108cc919061101d565b60405180910390fd5b565b6000600560009054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905081600560006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055508173ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a35050565b600073ffffffffffffffffffffffffffffffffffffffff168473ffffffffffffffffffffffffffffffffffffffff1603610a0f5760006040517fe602df05000000000000000000000000000000000000000000000000000000008152600401610a06919061101d565b60405180910390fd5b600073ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff1603610a815760006040517f94280d62000000000000000000000000000000000000000000000000000000008152600401610a78919061101d565b60405180910390fd5b81600160008673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020819055508015610b6e578273ffffffffffffffffffffffffffffffffffffffff168473ffffffffffffffffffffffffffffffffffffffff167f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b92584604051610b659190610f69565b60405180910390a35b50505050565b600073ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff1603610bc6578060026000828254610bba919061120f565b92505081905550610c99565b60008060008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002054905081811015610c52578381836040517fe450d38c000000000000000000000000000000000000000000000000000000008152600401610c49939291906111a9565b60405180910390fd5b8181036000808673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002081905550505b600073ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff1603610ce25780600260008282540392505081905550610d2f565b806000808473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020600082825401925050819055505b8173ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef83604051610d8c9190610f69565b60405180910390a3505050565b600081519050919050565b600082825260208201905092915050565b60005b83811015610dd3578082015181840152602081019050610db8565b60008484015250505050565b6000601f19601f8301169050919050565b6000610dfb82610d99565b610e058185610da4565b9350610e15818560208601610db5565b610e1e81610ddf565b840191505092915050565b60006020820190508181036000830152610e438184610df0565b905092915050565b600080fd5b600073ffffffffffffffffffffffffffffffffffffffff82169050919050565b6000610e7b82610e50565b9050919050565b610e8b81610e70565b8114610e9657600080fd5b50565b600081359050610ea881610e82565b92915050565b6000819050919050565b610ec181610eae565b8114610ecc57600080fd5b50565b600081359050610ede81610eb8565b92915050565b60008060408385031215610efb57610efa610e4b565b5b6000610f0985828601610e99565b9250506020610f1a85828601610ecf565b9150509250929050565b60008115159050919050565b610f3981610f24565b82525050565b6000602082019050610f546000830184610f30565b92915050565b610f6381610eae565b82525050565b6000602082019050610f7e6000830184610f5a565b92915050565b600080600060608486031215610f9d57610f9c610e4b565b5b6000610fab86828701610e99565b9350506020610fbc86828701610e99565b9250506040610fcd86828701610ecf565b9150509250925092565b600060ff82169050919050565b610fed81610fd7565b82525050565b60006020820190506110086000830184610fe4565b92915050565b61101781610e70565b82525050565b6000602082019050611032600083018461100e565b92915050565b60006020828403121561104e5761104d610e4b565b5b600061105c84828501610e99565b91505092915050565b600080600080600060a0868803121561108157611080610e4b565b5b600061108f88828901610ecf565b95505060206110a088828901610e99565b94505060406110b188828901610e99565b93505060606110c288828901610ecf565b92505060806110d388828901610ecf565b9150509295509295909350565b600080604083850312156110f7576110f6610e4b565b5b600061110585828601610e99565b925050602061111685828601610e99565b9150509250929050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052602260045260246000fd5b6000600282049050600182168061116757607f821691505b60208210810361117a57611179611120565b5b50919050565b60006040820190506111956000830185610f5a565b6111a26020830184610f5a565b9392505050565b60006060820190506111be600083018661100e565b6111cb6020830185610f5a565b6111d86040830184610f5a565b949350505050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052601160045260246000fd5b600061121a82610eae565b915061122583610eae565b925082820190508082111561123d5761123c6111e0565b5b9291505056fea2646970667358221220938c78db994d6089d1d11b08bcce7b6d00222b4f4f1868e947f2065a11b4b0c564736f6c634300081c0033";

    private static String librariesLinkedBinary;

    public static final String FUNC_ALLOWANCE = "allowance";

    public static final String FUNC_APPROVE = "approve";

    public static final String FUNC_BALANCEOF = "balanceOf";

    public static final String FUNC_DECIMALS = "decimals";

    public static final String FUNC_NAME = "name";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_RECORDTRADE = "recordTrade";

    public static final String FUNC_RENOUNCEOWNERSHIP = "renounceOwnership";

    public static final String FUNC_SYMBOL = "symbol";

    public static final String FUNC_TOTALSUPPLY = "totalSupply";

    public static final String FUNC_TRANSFER = "transfer";

    public static final String FUNC_TRANSFERFROM = "transferFrom";

    public static final String FUNC_TRANSFEROWNERSHIP = "transferOwnership";

    public static final String FUNC_TREASURY = "treasury";

    public static final CustomError ERC20INSUFFICIENTALLOWANCE_ERROR = new CustomError("ERC20InsufficientAllowance", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final CustomError ERC20INSUFFICIENTBALANCE_ERROR = new CustomError("ERC20InsufficientBalance", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final CustomError ERC20INVALIDAPPROVER_ERROR = new CustomError("ERC20InvalidApprover", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    public static final CustomError ERC20INVALIDRECEIVER_ERROR = new CustomError("ERC20InvalidReceiver", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    public static final CustomError ERC20INVALIDSENDER_ERROR = new CustomError("ERC20InvalidSender", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    public static final CustomError ERC20INVALIDSPENDER_ERROR = new CustomError("ERC20InvalidSpender", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    public static final CustomError OWNABLEINVALIDOWNER_ERROR = new CustomError("OwnableInvalidOwner", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    public static final CustomError OWNABLEUNAUTHORIZEDACCOUNT_ERROR = new CustomError("OwnableUnauthorizedAccount", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    public static final Event APPROVAL_EVENT = new Event("Approval", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event OWNERSHIPTRANSFERRED_EVENT = new Event("OwnershipTransferred", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event TRADERECORDED_EVENT = new Event("TradeRecorded", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event TRANSFER_EVENT = new Event("Transfer", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected StoToken(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected StoToken(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected StoToken(String contractAddress, Web3j web3j, TransactionManager transactionManager,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected StoToken(String contractAddress, Web3j web3j, TransactionManager transactionManager,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<ApprovalEventResponse> getApprovalEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(APPROVAL_EVENT, transactionReceipt);
        ArrayList<ApprovalEventResponse> responses = new ArrayList<ApprovalEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ApprovalEventResponse typedResponse = new ApprovalEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.owner = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.spender = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ApprovalEventResponse getApprovalEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(APPROVAL_EVENT, log);
        ApprovalEventResponse typedResponse = new ApprovalEventResponse();
        typedResponse.log = log;
        typedResponse.owner = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.spender = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<ApprovalEventResponse> approvalEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getApprovalEventFromLog(log));
    }

    public Flowable<ApprovalEventResponse> approvalEventFlowable(DefaultBlockParameter startBlock,
            DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(APPROVAL_EVENT));
        return approvalEventFlowable(filter);
    }

    public static List<OwnershipTransferredEventResponse> getOwnershipTransferredEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(OWNERSHIPTRANSFERRED_EVENT, transactionReceipt);
        ArrayList<OwnershipTransferredEventResponse> responses = new ArrayList<OwnershipTransferredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OwnershipTransferredEventResponse typedResponse = new OwnershipTransferredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.previousOwner = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.newOwner = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static OwnershipTransferredEventResponse getOwnershipTransferredEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(OWNERSHIPTRANSFERRED_EVENT, log);
        OwnershipTransferredEventResponse typedResponse = new OwnershipTransferredEventResponse();
        typedResponse.log = log;
        typedResponse.previousOwner = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.newOwner = (String) eventValues.getIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<OwnershipTransferredEventResponse> ownershipTransferredEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getOwnershipTransferredEventFromLog(log));
    }

    public Flowable<OwnershipTransferredEventResponse> ownershipTransferredEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(OWNERSHIPTRANSFERRED_EVENT));
        return ownershipTransferredEventFlowable(filter);
    }

    public static List<TradeRecordedEventResponse> getTradeRecordedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(TRADERECORDED_EVENT, transactionReceipt);
        ArrayList<TradeRecordedEventResponse> responses = new ArrayList<TradeRecordedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            TradeRecordedEventResponse typedResponse = new TradeRecordedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.tradeId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.seller = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.buyer = (String) eventValues.getIndexedValues().get(2).getValue();
            typedResponse.quantity = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.price = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static TradeRecordedEventResponse getTradeRecordedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(TRADERECORDED_EVENT, log);
        TradeRecordedEventResponse typedResponse = new TradeRecordedEventResponse();
        typedResponse.log = log;
        typedResponse.tradeId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.seller = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.buyer = (String) eventValues.getIndexedValues().get(2).getValue();
        typedResponse.quantity = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.price = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<TradeRecordedEventResponse> tradeRecordedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getTradeRecordedEventFromLog(log));
    }

    public Flowable<TradeRecordedEventResponse> tradeRecordedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(TRADERECORDED_EVENT));
        return tradeRecordedEventFlowable(filter);
    }

    public static List<TransferEventResponse> getTransferEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(TRANSFER_EVENT, transactionReceipt);
        ArrayList<TransferEventResponse> responses = new ArrayList<TransferEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            TransferEventResponse typedResponse = new TransferEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.from = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.to = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static TransferEventResponse getTransferEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(TRANSFER_EVENT, log);
        TransferEventResponse typedResponse = new TransferEventResponse();
        typedResponse.log = log;
        typedResponse.from = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.to = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<TransferEventResponse> transferEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getTransferEventFromLog(log));
    }

    public Flowable<TransferEventResponse> transferEventFlowable(DefaultBlockParameter startBlock,
            DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(TRANSFER_EVENT));
        return transferEventFlowable(filter);
    }

    public RemoteFunctionCall<BigInteger> allowance(String owner, String spender) {
        final Function function = new Function(FUNC_ALLOWANCE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, owner), 
                new org.web3j.abi.datatypes.Address(160, spender)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> approve(String spender, BigInteger value) {
        final Function function = new Function(
                FUNC_APPROVE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, spender), 
                new org.web3j.abi.datatypes.generated.Uint256(value)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> balanceOf(String account) {
        final Function function = new Function(FUNC_BALANCEOF, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, account)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> decimals() {
        final Function function = new Function(FUNC_DECIMALS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<String> name() {
        final Function function = new Function(FUNC_NAME, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> owner() {
        final Function function = new Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> recordTrade(BigInteger tradeId, String seller,
            String buyer, BigInteger quantity, BigInteger price) {
        final Function function = new Function(
                FUNC_RECORDTRADE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(tradeId), 
                new org.web3j.abi.datatypes.Address(160, seller), 
                new org.web3j.abi.datatypes.Address(160, buyer), 
                new org.web3j.abi.datatypes.generated.Uint256(quantity), 
                new org.web3j.abi.datatypes.generated.Uint256(price)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> renounceOwnership() {
        final Function function = new Function(
                FUNC_RENOUNCEOWNERSHIP, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> symbol() {
        final Function function = new Function(FUNC_SYMBOL, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<BigInteger> totalSupply() {
        final Function function = new Function(FUNC_TOTALSUPPLY, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> transfer(String to, BigInteger value) {
        final Function function = new Function(
                FUNC_TRANSFER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, to), 
                new org.web3j.abi.datatypes.generated.Uint256(value)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> transferFrom(String from, String to,
            BigInteger value) {
        final Function function = new Function(
                FUNC_TRANSFERFROM, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, from), 
                new org.web3j.abi.datatypes.Address(160, to), 
                new org.web3j.abi.datatypes.generated.Uint256(value)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> transferOwnership(String newOwner) {
        final Function function = new Function(
                FUNC_TRANSFEROWNERSHIP, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, newOwner)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> treasury() {
        final Function function = new Function(FUNC_TREASURY, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    @Deprecated
    public static StoToken load(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        return new StoToken(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static StoToken load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new StoToken(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static StoToken load(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        return new StoToken(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static StoToken load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new StoToken(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<StoToken> deploy(Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider, String _name, String _symbol,
            BigInteger _totalSupply, BigInteger _holdingSupply, String _issuer, String _treasury) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(_name), 
                new org.web3j.abi.datatypes.Utf8String(_symbol), 
                new org.web3j.abi.datatypes.generated.Uint256(_totalSupply), 
                new org.web3j.abi.datatypes.generated.Uint256(_holdingSupply), 
                new org.web3j.abi.datatypes.Address(160, _issuer), 
                new org.web3j.abi.datatypes.Address(160, _treasury)));
        return deployRemoteCall(StoToken.class, web3j, credentials, contractGasProvider, getDeploymentBinary(), encodedConstructor);
    }

    public static RemoteCall<StoToken> deploy(Web3j web3j, TransactionManager transactionManager,
            ContractGasProvider contractGasProvider, String _name, String _symbol,
            BigInteger _totalSupply, BigInteger _holdingSupply, String _issuer, String _treasury) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(_name), 
                new org.web3j.abi.datatypes.Utf8String(_symbol), 
                new org.web3j.abi.datatypes.generated.Uint256(_totalSupply), 
                new org.web3j.abi.datatypes.generated.Uint256(_holdingSupply), 
                new org.web3j.abi.datatypes.Address(160, _issuer), 
                new org.web3j.abi.datatypes.Address(160, _treasury)));
        return deployRemoteCall(StoToken.class, web3j, transactionManager, contractGasProvider, getDeploymentBinary(), encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<StoToken> deploy(Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit, String _name, String _symbol,
            BigInteger _totalSupply, BigInteger _holdingSupply, String _issuer, String _treasury) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(_name), 
                new org.web3j.abi.datatypes.Utf8String(_symbol), 
                new org.web3j.abi.datatypes.generated.Uint256(_totalSupply), 
                new org.web3j.abi.datatypes.generated.Uint256(_holdingSupply), 
                new org.web3j.abi.datatypes.Address(160, _issuer), 
                new org.web3j.abi.datatypes.Address(160, _treasury)));
        return deployRemoteCall(StoToken.class, web3j, credentials, gasPrice, gasLimit, getDeploymentBinary(), encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<StoToken> deploy(Web3j web3j, TransactionManager transactionManager,
            BigInteger gasPrice, BigInteger gasLimit, String _name, String _symbol,
            BigInteger _totalSupply, BigInteger _holdingSupply, String _issuer, String _treasury) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(_name), 
                new org.web3j.abi.datatypes.Utf8String(_symbol), 
                new org.web3j.abi.datatypes.generated.Uint256(_totalSupply), 
                new org.web3j.abi.datatypes.generated.Uint256(_holdingSupply), 
                new org.web3j.abi.datatypes.Address(160, _issuer), 
                new org.web3j.abi.datatypes.Address(160, _treasury)));
        return deployRemoteCall(StoToken.class, web3j, transactionManager, gasPrice, gasLimit, getDeploymentBinary(), encodedConstructor);
    }

    public static void linkLibraries(List<Contract.LinkReference> references) {
        librariesLinkedBinary = linkBinaryWithReferences(BINARY, references);
    }

    private static String getDeploymentBinary() {
        if (librariesLinkedBinary != null) {
            return librariesLinkedBinary;
        } else {
            return BINARY;
        }
    }

    public static class ApprovalEventResponse extends BaseEventResponse {
        public String owner;

        public String spender;

        public BigInteger value;
    }

    public static class OwnershipTransferredEventResponse extends BaseEventResponse {
        public String previousOwner;

        public String newOwner;
    }

    public static class TradeRecordedEventResponse extends BaseEventResponse {
        public BigInteger tradeId;

        public String seller;

        public String buyer;

        public BigInteger quantity;

        public BigInteger price;
    }

    public static class TransferEventResponse extends BaseEventResponse {
        public String from;

        public String to;

        public BigInteger value;
    }
}
