package com.exemple.historique.controller;

import com.exemple.historique.client.CalculClient;
import com.exemple.historique.model.Operation;
import com.exemple.historique.service.OperationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/operations")
public class OperationController {

    private static final List<String> OPERATEURS = List.of("somme", "soustraction", "produit", "division");

    private final OperationService operationService;
    private final CalculClient calculClient;

    public OperationController(OperationService operationService, CalculClient calculClient) {
        this.operationService = operationService;
        this.calculClient = calculClient;
    }

    private ResponseEntity<Object> calculer(Operation op) {
        if (!OPERATEURS.contains(op.getOperateur())) {
            return ResponseEntity.badRequest().body(Map.of("erreur", "Operateur invalide"));
        }
        try {
            double res = calculClient.calculer(op.getOperateur(), op.getA(), op.getB());
            op.setResultat(res);
            return null;
        } catch (HttpClientErrorException e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", "Division par zero impossible"));
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("erreur", "calcul-service est arrete ou injoignable"));
        }
    }

    @PostMapping
    public ResponseEntity<Object> creer(@RequestBody Operation op) {
        ResponseEntity<Object> erreur = calculer(op);
        if (erreur != null) return erreur;

        Operation sauvegardee = operationService.ajouter(op);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(sauvegardee.getId())
                .toUri();

        return ResponseEntity.created(location).body(sauvegardee);
    }

    @GetMapping
    public List<Operation> lister() {
        return operationService.lister();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Operation> lire(@PathVariable("id") int id) {
        Operation op = operationService.lire(id);
        if (op == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(op);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> modifier(@PathVariable("id") int id, @RequestBody Operation op) {
        if (!operationService.existe(id)) {
            return ResponseEntity.notFound().build();
        }

        ResponseEntity<Object> erreur = calculer(op);
        if (erreur != null) return erreur;

        operationService.remplacer(id, op);
        return ResponseEntity.ok(op);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable("id") int id) {
        Operation supprimee = operationService.supprimer(id);
        if (supprimee == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}