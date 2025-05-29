package com.mas.masonry;

import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Random;
import net.minecraft.world.damagesource.DamageSource;

public class AgentChatter {

    private static final Random CHAT_RANDOM = new Random();
    private static final Map<AgentEntity.AgentState, List<String>> STATE_CHAT_MESSAGES = new HashMap<>();

    static {
        STATE_CHAT_MESSAGES.put(AgentEntity.AgentState.IDLE, Arrays.asList(
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
                "Si paling noob lagi diem.",
                "Nggak tau mau ngapain lagi...",
                "Hmm, ada ide seru nggak?",
                "Nungguin malem biar bisa tidur...",
                "Bikin kopi dulu bentar..."
                
        ));
        STATE_CHAT_MESSAGES.put(AgentEntity.AgentState.WANDER, Arrays.asList(
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
        STATE_CHAT_MESSAGES.put(AgentEntity.AgentState.SEEK_RESOURCE, Arrays.asList(
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
        STATE_CHAT_MESSAGES.put(AgentEntity.AgentState.FLEE, Arrays.asList(
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
        STATE_CHAT_MESSAGES.put(AgentEntity.AgentState.ATTACK, Arrays.asList(
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
        STATE_CHAT_MESSAGES.put(AgentEntity.AgentState.HELP_ALLY, Arrays.asList(
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

        STATE_CHAT_MESSAGES.put(AgentEntity.AgentState.FIND_TARGET_BLOCK, Arrays.asList(
                "Nyari blok apa ya enaknya?",
                "Mulai pencarian blok target!",
                "Semoga nemu blok yang dicari...",
                "Scanning area untuk blok [NAMA_BLOK_TARGET]..." // Placeholder for dynamic block name
        ));

        STATE_CHAT_MESSAGES.put(AgentEntity.AgentState.MOVE_TO_TARGET_BLOCK, Arrays.asList(
                "Oke, blok target ketemu! OTW!",
                "Menuju ke lokasi blok...",
                "Gas ke bloknya, jangan sampai keduluan!",
                "Langkah tegap maju jalan ke blok!",
                "Semoga jalannya aman."
        ));

        STATE_CHAT_MESSAGES.put(AgentEntity.AgentState.HARVEST_BLOCK, Arrays.asList(
                "Saatnya panen blok! Hancurkan!",
                "Kerja keras demi sebongkah blok!",
                "Ayo hancurkan blok ini! Dapat item!",
                "Mining... mining... mining...",
                "Satu blok lagi buat koleksi!"
        ));
    }

    private static final List<String> DAMAGE_TAKEN_MESSAGES = Arrays.asList(
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
    );

    public static Component getFormattedChatMessage(AgentEntity.AgentState state, Component agentNameComponent) {
        List<String> messages = STATE_CHAT_MESSAGES.get(state);
        if (messages != null && !messages.isEmpty()) {
            String chatMessage = messages.get(CHAT_RANDOM.nextInt(messages.size()));
            // Placeholder for future: if (state == AgentEntity.AgentState.SEEK_RESOURCE && targetResource != null) { chatMessage = chatMessage.replace("[RESOURCE_TYPE]", targetResource.getName().getString()); }
            
            return Component.literal("<")
                            .append(agentNameComponent)
                            .append(Component.literal("> "))
                            .append(Component.literal(chatMessage));
        }
        return null; // Or a default component
    }

    public static Component getFormattedDamageTakenMessage(Component agentNameComponent, DamageSource source) {
        // For now, source is unused, but could be used later to customize messages
        if (!DAMAGE_TAKEN_MESSAGES.isEmpty()) {
            String chatMessage = DAMAGE_TAKEN_MESSAGES.get(CHAT_RANDOM.nextInt(DAMAGE_TAKEN_MESSAGES.size()));
            return Component.literal("<")
                            .append(agentNameComponent)
                            .append(Component.literal("> "))
                            .append(Component.literal(chatMessage));
        }
        return null;
    }
}
