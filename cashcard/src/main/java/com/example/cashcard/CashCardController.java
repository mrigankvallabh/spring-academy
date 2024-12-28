package com.example.cashcard;

import java.security.Principal;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/cashcards")
class CashCardController {
    private final CashCardRepository cashCardRepository;

    private CashCardController(CashCardRepository cashCardRepository) {
        this.cashCardRepository = cashCardRepository;
    }

    @GetMapping("/{requestId}")
    private ResponseEntity<CashCard> findById(@PathVariable Long requestId, Principal principal) {
        var cashcard = findCashCard(requestId, principal);

        if (cashcard != null) {
            return ResponseEntity.ok(cashcard);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping()
    private ResponseEntity<Iterable<CashCard>> findAll(Pageable pageable, Principal principal) {
        var page = cashCardRepository.findByOwner(
                principal.getName(),
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSortOr(Sort.by(Sort.Direction.ASC, "amount"))));
        return ResponseEntity.ok(page.getContent());
    }

    @PostMapping
    private ResponseEntity<Void> createCashCard(
            @RequestBody CashCard newCashCardRequest,
            UriComponentsBuilder ucb,
            Principal principal) {
        var newCashCardWithOwner = new CashCard(null, newCashCardRequest.amount(), principal.getName());
        var savedCard = cashCardRepository.save(newCashCardWithOwner);
        var locationURIofNewCard = ucb
                .path("/cashcards/{id}")
                .buildAndExpand(savedCard.id())
                .toUri();
        return ResponseEntity.created(locationURIofNewCard).build();
    }

    @PutMapping("/{requestId}")
    private ResponseEntity<Void> putCashCard(@PathVariable Long requestId,
            @RequestBody CashCard cashCardUpdate,
            Principal principal) {

        var cashcard = findCashCard(requestId, principal);
        if (cashcard != null) {
            var updatedCashCard = new CashCard(cashcard.id(), cashCardUpdate.amount(), principal.getName());
            cashCardRepository.save(updatedCashCard);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{requestId}")
    private ResponseEntity<Void> deleteCashCard(@PathVariable Long requestId, Principal principal) {
        if (!cashCardRepository.existsByIdAndOwner(requestId, principal.getName())) {
            return ResponseEntity.notFound().build();
        }
        cashCardRepository.deleteById(requestId);
        return ResponseEntity.noContent().build();
    }

    private CashCard findCashCard(Long id, Principal principal) {
        return cashCardRepository.findByIdAndOwner(id, principal.getName());
    }

}
