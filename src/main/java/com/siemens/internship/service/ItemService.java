package com.siemens.internship.service;

import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;

    private static ExecutorService executor = Executors.newFixedThreadPool(10);

    // removed unnecessary variable

    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }


    /**
     * The method returned before the background tasks were finished, so not all items were actually processed.
     * It also used shared variables without thread safety, had no proper error handling,
     * and misused the @Async annotation by returning a regular list instead of a CompletableFuture.
     */

    // Modified return type to CompletableFuture for solving Async issue
    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {
        List<Long> itemIds = itemRepository.findAllIds();

        // threadsafe list for processed items in list
        List<Item> processedItems = new CopyOnWriteArrayList<>();

        // A CompletableFuture feladatok listája
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Long id : itemIds) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(100);

                    // modified for readability
                    itemRepository.findById(id).ifPresent(item -> {
                        item.setStatus("PROCESSED");
                        itemRepository.save(item);
                        processedItems.add(item);
                    });

                } catch (InterruptedException e) {
                    // add error handling
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while processing item ID: " + id, e);
                } catch (Exception e) {
                    // add error handling
                    throw new RuntimeException("Error processing item ID: " + id, e);
                }
            }, executor);

            futures.add(future);
        }

        // wait until all async tasks finish
        return CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> processedItems);
    }
}

