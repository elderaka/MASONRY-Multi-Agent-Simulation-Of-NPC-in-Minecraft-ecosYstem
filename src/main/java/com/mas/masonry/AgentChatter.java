package com.mas.masonry;

import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Item;
import java.util.*;

public class AgentChatter {

    private static final Random CHAT_RANDOM = new Random();

    // Centralized static collection of all chat message categories
    private static final Map<String, List<String>> CHAT_MESSAGES = new HashMap<>();

    // Individual categories organized under a single static collection
    static {
        // Existing state-based messages...
        CHAT_MESSAGES.put("idle", Arrays.asList(
                "Woi, lagi nongkrong di sini dulu.",
                "Santuy dulu lah, pegel gerak mulu.",
                "Ngabsen dulu nih di map...",
                "Hmm, boring juga ya diem gini.",
                "Ada yang mau mabar kah?",
                "Lag pengen rebahan virtual.",
                "Nggak ada notif musuh aman...",
                "Cek-cek, koneksi aman?",
                "Nungguin ide bagus buat ngapain.",
                "Si paling chill di Minecraft.",
                "Lagi afk bentar nih...",
                "Santai dulu, capek habis mining.",
                "Nggak tau mau ngapain lagi...",
                "Hmm, ada ide seru nggak?",
                "Nungguin malem biar bisa tidur...",
                "Bikin kopi dulu bentar...",
                "Cek inventory...",
                "Lagi mikirin base mau dibikin kayak gimana...",
                "Ada yang butuh bantuan?",
                "Si paling noob lagi diem."
        ));

        CHAT_MESSAGES.put("wander", Arrays.asList(
                "Keliling map nyari village...",
                "Pengen lihat biome lain...",
                "Nyasar nih kayaknya...",
                "Ada yang lihat domba nggak?",
                "OTW ke koordinat XYZ...",
                "Pengen cari tempat buat bangun rumah...",
                "Lihat-lihat sekitar, siapa tau ada dungeon...",
                "Mencari benih gandum...",
                "Ada yang tahu ini biome apa?",
                "Si petualang lagi jalan-jalan.",
                "Keliling-keliling nyari spot asik.",
                "Jalan-jalan cuci mata digital.",
                "Nyasar kayaknya gue...",
                "Explore map dulu, siapa tau nemu harta karun.",
                "OTW nggak jelas nih ceritanya.",
                "Pengen gerak aja, biar nggak kaku.",
                "Lihat-lihat pemandangan blok-blok.",
                "Mencari jejak kehidupan...",
                "Ada yang tahu jalan pulang?",
                "Si petualang Minecraft dadakan."
        ));

        CHAT_MESSAGES.put("seek_resource", Arrays.asList(
                "Cari kayu buat bikin rumah masa depan.",
                "Butuh iron nih buat upgrade gear.",
                "Mata pencaharian seorang miner.",
                "Demi diamond dan emerald!",
                "Panen-panen dulu biar kaya.",
                "Nggak afdol main menkrep kalo nggak nyari resource.",
                "Perut kosong, eh, inventory kosong.",
                "Nyari makan virtual buat bertahan hidup.",
                "Ada yang tahu spot resource deket sini?",
                "Si pemburu resource sejati."
        ));

        CHAT_MESSAGES.put("flee", Arrays.asList(
                "KYAAAAA! Lariiiii!",
                "Ampun bang jago, ampun!",
                "Gue nggak ikut-ikutan!",
                "Mending cabut daripada kena mental.",
                "Nggak kuat gue lawan dia...",
                "Daripada mati konyol, mending ngacir.",
                "Bahaya banget woi di sana!",
                "Tolong ada monster serem!",
                "Jangan tinggalin gueee!"
        ));

        CHAT_MESSAGES.put("attack", Arrays.asList(
                "Maju terus pantang mundur!",
                "Rasain nih kekuatan blok!",
                "Jangan songong lu, mob!",
                "Demi keadilan dan kebenaran Minecraft!",
                "Prepare to get rekt!",
                "Sini lo kalo berani!",
                "Gue nggak takut sama lu!",
                "Saatnya jadi hero dadakan.",
                "Biarin gue yang urus!",
                "Si tukang gebuk mob.",
                "Hajar aja!",
                "Serbuuuuu!",
                "Rasain nih pukulan maut!",
                "Jangan biarin dia kabur!",
                "Kita kalahkan bersama!",
                "Sini lo monster jelek!",
                "Gue bantu serang!",
                "Saatnya bertarung!",
                "Awas kena pukul!",
                "Si pemberani (kalo rame-rame)."
        ));

        CHAT_MESSAGES.put("help_ally", Arrays.asList(
                "Woi, sini gue bantuin!",
                "Gaspol bantu teman!",
                "Nggak boleh biarin teman sendirian!",
                "Kita tim solid, bro!",
                "Hold on, I'm coming!",
                "Butuh bantuan? Gue standby!",
                "Jangan takut, ada gue di sini.",
                "Saling bantu itu indah.",
                "Bareng-bareng kita bisa!",
                "Si support system terbaik di Minecraft.",
                "Bantuin nih!",
                "Sini gue cover!",
                "Jangan panik, gue datang!",
                "Kita lawan bareng!",
                "Need backup?",
                "Gue bantu heal!",
                "Jangan mati dulu!"
        ));

        CHAT_MESSAGES.put("find_target_block", Arrays.asList(
                "Nyari blok apa ya enaknya?",
                "Mulai pencarian blok target!",
                "Semoga nemu blok yang dicari...",
                "Scanning area untuk blok yang dibutuhin..."
        ));

        CHAT_MESSAGES.put("move_to_target_block", Arrays.asList(
                "Oke, blok target ketemu! OTW!",
                "Menuju ke lokasi blok...",
                "Gas ke bloknya, jangan sampai keduluan!",
                "Langkah tegap maju jalan ke blok!",
                "Semoga jalannya aman."
        ));

        CHAT_MESSAGES.put("harvest_block", Arrays.asList(
                "Saatnya panen blok! Hancurkan!",
                "Kerja keras demi sebongkah blok!",
                "Ayo hancurkan blok ini! Dapat item!",
                "Mining... mining... mining...",
                "Satu blok lagi buat koleksi!"
        ));

        CHAT_MESSAGES.put("place_construction_block", Arrays.asList(
                "Bangun rumah little by little!",
                "Pasang blok satu per satu biar rapih.",
                "Slow but sure bikin bangunan.",
                "Architect mode: ON!",
                "Lagi vibes bangun istana nih.",
                "Semoga nggak salah taruh blok...",
                "Building in progress, jangan ganggu!",
                "Lagi fokus bangun base impian.",
                "Satu blok = satu langkah ke surga.",
                "Konsisten bangun, nanti jadi mansion!"
        ));

        // === NEW RESOURCE SHARING STATES ===

        CHAT_MESSAGES.put("request_item_from_agent", Arrays.asList(
                "Eh bro, lu punya {ITEM} nggak?",
                "Bestie, boleh minjem {ITEM} bentar?",
                "Guys, ada yang punya {ITEM} spare?",
                "Tolong dong, gue butuh {ITEM} banget nih!",
                "Halo kak, bisa share {ITEM} nggak?",
                "Woi, {TARGET_NAME}, lu ada {ITEM} lebih?",
                "Permisi, boleh minta {ITEM} dikit?",
                "Bro, gue lagi desperate butuh {ITEM}...",
                "Ada yang baik hati mau kasih {ITEM}?",
                "Please, anyone punya {ITEM}? Urgent!",
                "Guys help! Butuh {ITEM} buat project gue.",
                "Sharing is caring, ada {ITEM} nggak?",
                "Boleh dong dibantu, butuh {ITEM} nih.",
                "Siapa yang dermawan mau kasih {ITEM}?",
                "Emergency! Gue butuh {ITEM} sekarang juga!"
        ));

        CHAT_MESSAGES.put("give_item_to_agent", Arrays.asList(
                "Nih bro, ambil aja {ITEM}nya!",
                "Gue kasih lu {ITEM}, no problema!",
                "Sharing time! Ini dia {ITEM}nya.",
                "Take it bestie, semoga membantu!",
                "Gratis! Ambil {ITEM}nya ya.",
                "Gue ada lebihan {ITEM}, lu mau?",
                "Support local friend! Ini {ITEM}nya.",
                "Gotong royong mode, nih {ITEM}!",
                "Buat lu deh {ITEM}nya, good luck!",
                "Ada rezeki {ITEM}, gue bagi-bagi!",
                "Friendship goals: sharing {ITEM}!",
                "Teamwork makes dream work! Ini {ITEM}.",
                "Gue helpful banget kan? Nih {ITEM}!",
                "Community support! Take the {ITEM}.",
                "Dengan senang hati, ini {ITEM}nya!"
        ));

        CHAT_MESSAGES.put("wait_for_resource_delivery", Arrays.asList(
                "Nungguin bantuan dari temen nih...",
                "Hope someone bisa bantuin gue.",
                "Waiting mode: activated.",
                "Semoga ada yang baik hati help.",
                "Lagi nunggu delivery dari bestie.",
                "Fingers crossed ada yang mau share!",
                "Ditunggu ya guys, butuh bantuan nih.",
                "Standing by, nunggu resource datang.",
                "Please jangan ignore request gue...",
                "Hopefully teamwork gonna work!",
                "Nunggu dengan penuh harapan.",
                "Semoga komunitas solid bantu gue.",
                "Waiting patiently buat support.",
                "Tolong dong, jangan biarin gue sendirian.",
                "Trust the process, nunggu bantuan."
        ));

        CHAT_MESSAGES.put("consider_resource_request", Arrays.asList(
                "Hmm, apa gue bisa bantuin ya?",
                "Let me check inventory dulu...",
                "Mikir-mikir, gue ada spare nggak ya?",
                "Coba liat dulu punya apa aja.",
                "Bantuin atau nggak ya? Dilema nih.",
                "Check stock dulu before commit.",
                "Gue baik hati nggak sih hari ini?",
                "Calculating... apakah gue generous?",
                "Mood sharing lagi on nggak ya?",
                "Pertimbangan mature: help or not?",
                "Lagi good mood, maybe bisa help.",
                "Checking generosity level...",
                "Gue type orang yang suka share nggak?",
                "Hmm, karma points atau self interest?",
                "Decision time: be helpful or nah?"
        ));

        CHAT_MESSAGES.put("deliver_resource_to_agent", Arrays.asList(
                "OTW bawa barang pesanan!",
                "Delivery service on the way!",
                "Gue jadi kurir dadakan nih.",
                "Express delivery coming through!",
                "Helping hand mode: active!",
                "Gaspol antar barang ke temen!",
                "Good karma delivery in progress.",
                "Being helpful feels good ya!",
                "Community service time!",
                "Delivery man of the year: me!",
                "Gotong royong spirit activated!",
                "Friendly neighborhood helper coming!",
                "Support system on the move!",
                "Teamwork delivery express!",
                "Spreading good vibes sama barang!"
        ));

        // === ENHANCED SOCIAL STATES ===

        CHAT_MESSAGES.put("chat_with_agent", Arrays.asList(
                "Ngobrol yuk, lagi gabut nih!",
                "Hai bestie, gimana kabarnya?",
                "Woi, mau ngobrolin apa nih?",
                "Chit chat time with the homies!",
                "Socializing is good for mental health!",
                "Ayo sharing cerita seru!",
                "Quality time dengan temen-temen.",
                "Bosan sendirian, mau ngobrol!",
                "Story time! Ada gossip baru nggak?",
                "Let's catch up, udah lama nggak ngobrol.",
                "Small talk can lead to big friendship!",
                "Mood ngobrol santai banget nih.",
                "Connection time dengan squad!",
                "Bonding session dimulai!",
                "Social battery: fully charged!"
        ));

        CHAT_MESSAGES.put("greet_agent", Arrays.asList(
                "Halo! Apa kabar?",
                "Eh, ketemu teman di sini!",
                "Woi, lagi ngapain?",
                "Hai! Salam kenal ya.",
                "Halo kawan! Selamat datang!",
                "Ketemu lagi nih!",
                "Senang bertemu denganmu!",
                "Apa kabar? Baik-baik saja?",
                "Oh, hai! Sudah lama tidak jumpa!",
                "Salam hangat untukmu!",
                "Halo {TARGET_NAME}! Apa kabar?",
                "Hai {TARGET_NAME}, lagi ngapain nih?",
                "Woi {TARGET_NAME}, gimana harinya?",
                "Lama nggak ketemu {TARGET_NAME}!",
                "Eh, ada {TARGET_NAME}! Mau jalan bareng?",
                "Halo halo {TARGET_NAME}! Ketemu lagi kita.",
                "{TARGET_NAME}! Ke mana aja nih?",
                "Woi {TARGET_NAME}, baru bangun ya?",
                "Ketemu {TARGET_NAME} lagi nih, dunia kecil ya?",
                "Hai {TARGET_NAME}, mau bantuin nggak?"
        ));

        // Continue with existing messages...
        CHAT_MESSAGES.put("damage_taken", Arrays.asList(
                "Aduuuh! Sakit woy!",
                "Waduh, kena serang nih!",
                "Ouch! Siapa sih yang mukul?!",
                "Eh, eh, kok sakit?",
                "Ampun dah, jangan diserang mulu!",
                "Aww, nyerinya tuh di sini!",
                "Stop! Gue lagi nggak mood berantem!",
                "Duh, HP gue berkurang!",
                "Ini siapa yang iseng sih?",
                "Awas ya lu! Gue bales nanti!"
        ));

        CHAT_MESSAGES.put("danger_warning", Arrays.asList(
                "Hati-hati! Ada bahaya di dekat sini!",
                "Awas! Musuh terdeteksi!",
                "Bahaya! Jangan mendekat ke arah sini!",
                "Lari! Ada monster berbahaya!",
                "Mundur! Situasi tidak aman!",
                "Cepat pergi! Tidak aman di sini!",
                "Awas serangan! Siapkan pertahanan!",
                "Bahaya level tinggi terdeteksi! Hati-hati!",
                "Monster kuat mendekat! Jangan sendirian!",
                "Siaga penuh! Area ini berbahaya!"
        ));

        CHAT_MESSAGES.put("chat_initiator", Arrays.asList(
                "Kamu suka build apa sih biasanya?",
                "Ada rencana buat base nggak?",
                "Eh tadi kayaknya aku lihat diamond deh...",
                "Kapan-kapan kita adventure bareng yuk!",
                "Kamu lagi ngumpulin resource apa?",
                "Ada mobs aneh di sebelah sana, hati-hati.",
                "Cuacanya lagi bagus nih buat ngerjain farm.",
                "Katanya mau ada update baru ya?",
                "Bosen juga ya kadang-kadang di dunia kotak.",
                "Aku mau bikin rumah deket sungai kayaknya.",
                "Kamu udah pernah ke nether belum?",
                "Sebenernya aku takut banget sama creeper.",
                "Kepengen bikin automatic farm deh.",
                "Kalo malem suka pada spawn zombie ya.",
                "Ada ide buat build yang keren nggak?"
        ));

        CHAT_MESSAGES.put("chat_response", Arrays.asList(
                "Oh iya? Aku lebih suka bikin farm sih.",
                "Hmm, belum kepikiran. Ada saran?",
                "Serius? Di mana? Aku juga lagi nyari diamond!",
                "Boleh banget! Aku juga pengen explore lebih jauh.",
                "Lagi ngumpulin kayu sama batu, mau bikin base.",
                "Makasih infonya, aku bakal lebih hati-hati.",
                "Bener, cuaca bagus emang enak buat farming.",
                "Aku juga dengar itu, katanya bagus banget.",
                "Hahaha, tapi kadang aku suka banget dunia kotak ini.",
                "Bagus tuh! Dekat sungai pemandangannya keren.",
                "Udah, tapi selalu takut ketemu ghast.",
                "Sama! Creeper itu musuh paling nyebelin!",
                "Aku bisa bantu kalo mau bikin farm.",
                "Iya, makanya aku selalu tidur tiap malem.",
                "Coba bikin menara tinggi, itu selalu keren!"
        ));

        // === RESOURCE SPECIFIC MESSAGES ===

        CHAT_MESSAGES.put("resource_request_accepted", Arrays.asList(
                "Oke bro, gue bantuin!",
                "Siap! Gue ada nih.",
                "No problem, gue kasih!",
                "Ayo sharing is caring!",
                "Gue support lu 100%!",
                "With pleasure, bro!",
                "Community first! Gue bantuin.",
                "Tenang, gue ada backup.",
                "Friendship goals activated!",
                "Gue baik hati kan? Wkwk."
        ));

        CHAT_MESSAGES.put("resource_request_declined", Arrays.asList(
                "Sorry bro, gue lagi kosong juga...",
                "Maaf ya, lagi butuh buat diri sendiri.",
                "Wah, gue nggak punya deh...",
                "Sorry, stock gue habis nih.",
                "Maaf banget, lagi krisis juga.",
                "Gue pengen bantuin tapi nggak bisa...",
                "Next time ya, sekarang lagi susah.",
                "Maaf, priorities gue beda nih.",
                "Sorry bestie, self preservation mode.",
                "Gue juga lagi ngumpulin yang sama..."
        ));
        CHAT_MESSAGES.put("share_gossip", Arrays.asList(
                "Eh, ada cerita menarik nih!",
                "Gossip time! Lu udah denger belum?",
                "Ada info fresh dari gue nih!",
                "Pssst, ini rahasia ya... tapi gue share deh!",
                "Breaking news dari komunitas kita!",
                "Lu harus tau info ini sih!",
                "Kata orang-orang nih...",
                "Gue denger-denger ada kabar...",
                "Update terbaru dari sekitar sini!",
                "Spreading the word, check this out!"
        ));

        CHAT_MESSAGES.put("react_to_gossip", Arrays.asList(
                "Serius?! Gue baru tau nih!",
                "Wah, thanks for the info!",
                "Interesting banget gosipnya!",
                "Good to know, makasih ya!",
                "Oh gitu toh ceritanya...",
                "Noted! Gue simpen deh infonya.",
                "Waduh, gue harus hati-hati nih.",
                "Thanks for the heads up!",
                "Berguna banget nih informasinya!",
                "Gue bakal spread the word juga deh!"
        ));

// === TRADING MESSAGES ===
        CHAT_MESSAGES.put("propose_trade", Arrays.asList(
                "Eh, mau tukeran nggak nih?",
                "Ada tawaran menarik buat lu!",
                "Trading time! Interested nggak?",
                "Gue ada deal bagus nih...",
                "Mau barter-barteran yuk!",
                "Tukar tambah, gimana?",
                "Ada yang lu butuhin? Gue bisa supply!",
                "Let's make a deal!",
                "Mutual benefit trade, mau nggak?",
                "Gue rasa ini win-win solution!"
        ));

        CHAT_MESSAGES.put("accept_trade", Arrays.asList(
                "Deal! Gue setuju!",
                "Sounds good, let's do it!",
                "Oke, fair trade nih!",
                "Agreed! Win-win banget!",
                "Perfect, exactly what I needed!",
                "Mantap! Trade yang bagus!",
                "Yep, gue terima tawarannya!",
                "Cool, beneficial buat kita berdua!",
                "Excellent trade proposal!",
                "Gue suka dealnya, ayo!"
        ));

        CHAT_MESSAGES.put("decline_trade", Arrays.asList(
                "Sorry bro, nggak cocok sama gue...",
                "Thanks, tapi gue pass deh.",
                "Hmm, maybe next time ya?",
                "Nggak match sama kebutuhan gue sih.",
                "Appreciate the offer, tapi no thanks.",
                "Gue lagi nggak butuh itu deh.",
                "Sorry, tapi gue decline ya.",
                "Not really interested, sorry!",
                "Thanks anyway, but I'll pass.",
                "Maybe we can find better deal later?"
        ));

        CHAT_MESSAGES.put("trade_completed", Arrays.asList(
                "Perfect! Thanks for the trade!",
                "Smooth transaction, bro!",
                "Pleasure doing business!",
                "Mantap! Fair trade banget!",
                "Thanks! This really helps!",
                "Excellent! Both happy nih!",
                "Good deal, thanks partner!",
                "Sukses! Trade yang memuaskan!",
                "Awesome! Until next trade!",
                "Perfect exchange, appreciate it!"
        ));

        // === REPUTATION GOSSIP ===
        CHAT_MESSAGES.put("positive_reputation", Arrays.asList(
                "{SUBJECT} itu reliable trader banget!",
                "Si {SUBJECT} helpful banget orangnya!",
                "{SUBJECT} always fair in trades!",
                "Gue recommend {SUBJECT}, trustworthy!",
                "{SUBJECT} orangnya generous, good person!",
                "Fair trader: {SUBJECT}, recommended!",
                "{SUBJECT} itu team player sejati!",
                "Quality person si {SUBJECT}, gue suka!",
                "{SUBJECT} never disappoints in business!",
                "Si {SUBJECT} itu role model trader!"
        ));

        CHAT_MESSAGES.put("negative_reputation", Arrays.asList(
                "{SUBJECT} agak susah diajak deal...",
                "Hati-hati trade sama {SUBJECT} ya...",
                "Si {SUBJECT} suka ngambil untung berlebihan.",
                "{SUBJECT} sometimes unreliable sih...",
                "Better double-check kalo deal sama {SUBJECT}.",
                "{SUBJECT} agak selfish in trades.",
                "Gue kurang suka attitude {SUBJECT}...",
                "{SUBJECT} suka ngasih deal yang unfair.",
                "Risky trader: {SUBJECT}, be careful!",
                "Si {SUBJECT} kurang team spirit."
        ));

    }

    /**
     * Gets a formatted chat message based on agent state with item context
     */
    public static Component getFormattedChatMessage(AgentEntity.AgentState state, Component agentNameComponent, Item contextItem, String targetName) {
        String stateKey = state.name().toLowerCase();
        List<String> messages = CHAT_MESSAGES.get(stateKey);

        if (messages == null || messages.isEmpty()) {
            messages = CHAT_MESSAGES.get("idle");
            MASONRY.LOGGER.debug("No chat message found for state: {}, using idle messages", state);
        }

        String chatMessage = messages.get(CHAT_RANDOM.nextInt(messages.size()));

        // Replace placeholders
        if (contextItem != null) {
            String itemName = contextItem.getDescription().getString();
            chatMessage = chatMessage.replace("{ITEM}", itemName);
        }

        if (targetName != null) {
            chatMessage = chatMessage.replace("{TARGET_NAME}", targetName);
        }

        return Component.literal("<")
                .append(agentNameComponent)
                .append(Component.literal("> "))
                .append(Component.literal(chatMessage));
    }

    /**
     * Gets a formatted chat message based on agent state (original method)
     */
    public static Component getFormattedChatMessage(AgentEntity.AgentState state, Component agentNameComponent) {
        return getFormattedChatMessage(state, agentNameComponent, null, null);
    }

    /**
     * Gets a resource-specific chat message for requests/sharing
     */
    public static Component getResourceChatMessage(AgentEntity agent, String messageType, Item item, String targetName) {
        List<String> messages = CHAT_MESSAGES.get(messageType);
        if (messages == null || messages.isEmpty()) {
            return getFormattedChatMessage(AgentEntity.AgentState.IDLE, Component.literal(agent.getBaseName()));
        }

        String chatMessage = messages.get(CHAT_RANDOM.nextInt(messages.size()));

        // Replace placeholders
        if (item != null) {
            String itemName = item.getDescription().getString();
            chatMessage = chatMessage.replace("{ITEM}", itemName);
        }

        if (targetName != null) {
            chatMessage = chatMessage.replace("{TARGET_NAME}", targetName);
        }

        return Component.literal("<")
                .append(Component.literal(agent.getBaseName()))
                .append(Component.literal("> "))
                .append(Component.literal(chatMessage));
    }

    /**
     * Enhanced method for resource-aware conversations
     */
    public static Component getResourceAwareConversationMessage(AgentEntity speaker, AgentEntity listener, boolean isInitiator) {
        // Check if speaker needs resources
        if (speaker.getResourceRequestSystem().hasNeededItems()) {
            Map<Item, Integer> needed = speaker.getResourceRequestSystem().getNeededItems();
            Item firstNeeded = needed.keySet().iterator().next();

            if (isInitiator && CHAT_RANDOM.nextFloat() < 0.6f) { // 60% chance to ask about resources
                return getResourceChatMessage(speaker, "request_item_from_agent", firstNeeded, listener.getBaseName());
            }
        }

        // Check if listener needs resources and speaker can help
        if (listener.getResourceRequestSystem().hasNeededItems() && !isInitiator) {
            Map<Item, Integer> needed = listener.getResourceRequestSystem().getNeededItems();
            Item firstNeeded = needed.keySet().iterator().next();

            // Check if speaker has the item
            boolean hasItem = false;
            for (int i = 0; i < speaker.getInventory().getContainerSize(); i++) {
                if (speaker.getInventory().getItem(i).getItem() == firstNeeded) {
                    hasItem = true;
                    break;
                }
            }

            if (hasItem && CHAT_RANDOM.nextFloat() < 0.4f) { // 40% chance to offer help
                return getResourceChatMessage(speaker, "give_item_to_agent", firstNeeded, listener.getBaseName());
            }
        }

        // Fallback to regular conversation
        return getConversationMessage(speaker, listener, isInitiator);
    }

    /**
     * Gets a formatted damage taken message
     */
    public static Component getFormattedDamageTakenMessage(Component agentNameComponent, DamageSource source) {
        List<String> messages = CHAT_MESSAGES.get("damage_taken");
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        String chatMessage = messages.get(CHAT_RANDOM.nextInt(messages.size()));
        return Component.literal("<")
                .append(agentNameComponent)
                .append(Component.literal("> "))
                .append(Component.literal(chatMessage));
    }

    /**
     * Gets a random message of a specific type
     */
    public static String getRandomMessage(String type) {
        List<String> messages = CHAT_MESSAGES.get(type);
        if (messages == null || messages.isEmpty()) {
            return "Hello!"; // Default fallback
        }
        return messages.get(CHAT_RANDOM.nextInt(messages.size()));
    }

    /**
     * Gets a formatted communication message between agents
     */
    public static Component getAgentCommunicationMessage(AgentEntity agent, AgentEntity.CommunicationType messageType, String targetName) {
        String agentName = agent.getBaseName();
        Component agentNameComponent = Component.literal(agentName);

        String messageKey;
        switch (messageType) {
            case GREETING:
                messageKey = "greet_agent";
                break;
            case DANGER_WARNING:
                messageKey = "danger_warning";
                break;
            case RESOURCE_SHARING:
                messageKey = "give_item_to_agent";
                break;
            default:
                messageKey = "greet_agent";
        }

        String message = getRandomMessage(messageKey);

        // Replace placeholders if needed
        if (targetName != null) {
            message = message.replace("{TARGET_NAME}", targetName);
        }

        return Component.literal("<")
                .append(agentNameComponent)
                .append(Component.literal("> "))
                .append(Component.literal(message));
    }

    /**
     * Gets a conversation message for the ChatWithAgent state
     */
    public static Component getConversationMessage(AgentEntity speaker, AgentEntity listener, boolean isInitiator) {
        String messageType = isInitiator ? "chat_initiator" : "chat_response";
        String message = getRandomMessage(messageType);

        // Sometimes address the other agent by name
        if (CHAT_RANDOM.nextFloat() < 0.3f) {
            message = listener.getBaseName() + ", " + message;
        }

        return Component.literal("<")
                .append(Component.literal(speaker.getBaseName()))
                .append(Component.literal("> "))
                .append(Component.literal(message));
    }
}