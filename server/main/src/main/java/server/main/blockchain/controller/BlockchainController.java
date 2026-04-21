//package server.main.blockchain.controller;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//import server.main.blockchain.service.BlockchainWorkerService;
//
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("blockchain")
//public class BlockchainController {
//
//    private final BlockchainWorkerService blockchainWorkerService;
//
//    @PostMapping("/process")
//    public ResponseEntity<Void> processPending() {
//        blockchainWorkerService.processPending();
//        return ResponseEntity.ok().build();
//    }
//}
