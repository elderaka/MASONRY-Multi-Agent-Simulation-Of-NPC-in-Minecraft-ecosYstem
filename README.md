# MASONRY: Multi-Agent Simulation Of NPC in Minecraft Ecosystem

Welkam to MASONRY

## Deskripsi Proyek

MASONRY, yang merupakan singkatan dari **Multi-Agent Simulation Of NPC in Minecraft ecosystem**, bertujuan untuk membuat simulasi kehidupan agen cerdas di dalam Minecraft. Agen-agen ini berbeda dari NPC biasa yang cenderung statis. Mereka memiliki tujuan, dapat mengambil keputusan secara mandiri, dan mampu berkolaborasi

Agen pada proyek ini dapat:
* **Membangun rumah**: Mulai dari mengumpulkan kayu dan batu hingga membangun rumah yang utuh.
* **Mencari Resource**: Bukan cuman untuk kebutuhan pribadi, tetapi juga untuk kepentingan bersama tim.
* **Merespons lingkungan**: Mereka akan menghindari bahaya dan membantu agen lain yang sedang kesulitan.
* **Berkomunikasi (dasar)**: Untuk membuat simulasi lebih hidup, agen akan mengirimkan pesan chat saat melakukan tindakan atau berubah status.

Proyek ini dibuat untuk menunjukkan bagaimana simulasi agen cerdas dapat diterapkan dalam game yang kompleks seperti Minecraft.


## Tujuan Proyek (Goals)

Tujuan utama proyek MASONRY adalah:
1.  **Implementasi Perilaku Mirip Manusia**:
    * Membuat agen membangun dengan kecepatan yang lebih realistis (tidak instan).
    * Memungkinkan agen menghancurkan blok (untuk mendapatkan bahan atau membersihkan area).
    * Mengembangkan kemampuan agen untuk mendeteksi alat di inventaris mereka dan menggunakannya secara efektif (misalnya, menggunakan kapak untuk menebang pohon lebih cepat).
2.  **Kolaborasi Multi-Agen**:
    * Mengembangkan sistem di mana beberapa agen dapat bekerja sama untuk membangun struktur sederhana (misalnya, rumah kecil) dari awal. Ini memerlukan:
        * Pengumpulan sumber daya.
        * Manajemen inventaris.
        * Penempatan blok yang terkoordinasi.
        * Komunikasi atau kesadaran antar agen.
3.  **Interaksi Sosial (Dasar)**:
    * Membuat agen dapat mendeteksi agen lain di sekitarnya.
    * Mengimplementasikan kemampuan agen untuk membantu teman yang diserang atau dalam bahaya.
    * Mengizinkan agen untuk berkomunikasi atau memberikan status melalui chat.


## Struktur Data Utama (Data Structures)

Secara umum, data penting buat tiap agen disimpen di kelas `AgentEntity.java` dan kelas `AgentMemory` (inner class di `AgentEntity`). Beberapa hal penting:

*   **`AgentState` (enum)**: Menentukan Agen lagi ngapain (misal, `IDLE`, `WANDER`, `FLEE`, `PLACE_CONSTRUCTION_BLOCK`, `HARVEST_BLOCK`, dll).
*   **`AgentMemory`**:
    *   `ticksSinceLastBlockPlace`: Ngitung waktu jeda antar penempatan blok.
    *   `ticksInCurrentState`: Ngitung udah berapa lama agen ada di state sekarang (dipake buat timing, misal pas hancurin blok).
    *   `currentState`: Nyimpen state agen saat ini.
    *   Informasi target: `targetBlockPos`, `targetPos`, `targetEntity`, dll.
    *   Status internal: `hungerLevel`, `healthPercent`, `fearLevel`, `socialMeter`.
*   **`inventory` (SimpleContainer)**: Nyimpen barang-barang yang dipunya agen (9 slot).
*   **Blueprint (List<BlueprintBlock>)**: Rencana bangunan yang mau dibuat agen. Saat ini ada `MASONRY.SIMPLE_WALL_BLUEPRINT`.
*   **Konstanta Penting di `AgentEntity.java`**:
    *   `MIN_TICKS_BETWEEN_PLACEMENT`: Jeda minimal antar penempatan blok.
    *   `TICKS_TO_HARVEST_BLOCK`: Waktu yang dibutuhin buat hancurin satu blok.

## Anggota Tim
*   Lauda Dhia Raka
*   Imtitsal Ulya Salsabila
*   Nicholas
*   Hanif Athalla Mahardika Haranto

---
*README ini akan terus diupdate seiring perkembangan proyek.*
