package com.gopal.controller;

import com.gopal.payload.database.DatabaseDetails;
import com.gopal.services.DynamicEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/database")
public class DynamicEntityController {

    @Autowired
    private DynamicEntityService dynamicEntityService;

//    @PostMapping("/setDataSource")
    @PostMapping("/connect")
    public ResponseEntity<?> setDataSource(@RequestBody DatabaseDetails details) {
        try {
            dynamicEntityService.setDataSource(details);
            return ResponseEntity.ok("Data source set and entities generated successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/{tableName}")
    public ResponseEntity<?> findAll(@PathVariable("tableName") String tableName) {
        try {
        	System.out.println("Find All method for table :"+tableName);
            return ResponseEntity.ok(dynamicEntityService.findAll(tableName));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/{tableName}/{id}")
    public ResponseEntity<?> findById(@PathVariable String tableName, @PathVariable Object id) {
        try {
            return ResponseEntity.ok(dynamicEntityService.findById(tableName, id));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/{tableName}")
    public ResponseEntity<?> save(@PathVariable String tableName, @RequestBody Object entity) {
        try {
            return ResponseEntity.ok(dynamicEntityService.save(tableName, entity));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @DeleteMapping("/{tableName}/{id}")
    public ResponseEntity<?> deleteById(@PathVariable String tableName, @PathVariable Object id) {
        try {
            dynamicEntityService.deleteById(tableName, id);
            return ResponseEntity.ok("Entity deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
