package com.rebirth.my.config;

import com.rebirth.my.wardrobe.WardrobeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class MilvusDataInitializer implements CommandLineRunner {

    @Autowired
    private WardrobeService wardrobeService;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("==========================================");
        System.out.println("🚀 Checking Milvus Data Synchronization...");
        System.out.println("==========================================");

        // [OPTIMIZATION] Full sync disabled for faster startup
        // Real-time sync happens when clothes are registered via
        // WardrobeService.saveClothes()
        // Uncomment below line ONLY if you need to force a full re-sync:
        wardrobeService.syncAllDataToMilvus();

        System.out.println("⚡ Fast startup mode: Real-time sync enabled.");
        System.out.println("   (Clothes are synced to Milvus when registered)");

        System.out.println("==========================================");
        System.out.println("✅ Milvus Data Initialization Finished.");
        System.out.println("==========================================");
    }
}
