package com.struyen.struyen.config;

import com.struyen.struyen.entities.Category;
import com.struyen.struyen.entities.Role;
import com.struyen.struyen.entities.User;
import com.struyen.struyen.repositories.CategoryRepository;
import com.struyen.struyen.repositories.RoleRepository;
import com.struyen.struyen.repositories.StoryRepository;
import com.struyen.struyen.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final StoryRepository storyRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Map<String, String[]> BILINGUAL_CATEGORIES = Map.ofEntries(
            Map.entry("4-koma", new String[]{"4-Koma", "4-Koma"}),
            Map.entry("action", new String[]{"Action", "Hành Động"}),
            Map.entry("adaptation", new String[]{"Adaptation", "Chuyển Thể"}),
            Map.entry("adventure", new String[]{"Adventure", "Phiêu Lưu"}),
            Map.entry("aliens", new String[]{"Aliens", "Người Ngoài Hành Tinh"}),
            Map.entry("animals", new String[]{"Animals", "Động Vật"}),
            Map.entry("anthology", new String[]{"Anthology", "Tuyển Tập"}),
            Map.entry("award-winning", new String[]{"Award Winning", "Đoạt Giải Thưởng"}),
            Map.entry("boys-love", new String[]{"Boys' Love", "Đam Mỹ"}),
            Map.entry("comedy", new String[]{"Comedy", "Hài Hước"}),
            Map.entry("cooking", new String[]{"Cooking", "Ẩm Thực"}),
            Map.entry("crime", new String[]{"Crime", "Tội Phạm"}),
            Map.entry("crossdressing", new String[]{"Crossdressing", "Giả Trang"}),
            Map.entry("delinquents", new String[]{"Delinquents", "Bất Hảo / Nổi Loạn"}),
            Map.entry("demons", new String[]{"Demons", "Ma Quỷ"}),
            Map.entry("doujinshi", new String[]{"Doujinshi", "Doujinshi"}),
            Map.entry("drama", new String[]{"Drama", "Kịch Tính"}),
            Map.entry("fan-colored", new String[]{"Fan Colored", "Màu Fan-made"}),
            Map.entry("fantasy", new String[]{"Fantasy", "Giả Tưởng"}),
            Map.entry("full-color", new String[]{"Full Color", "Truyện Màu"}),
            Map.entry("genderswap", new String[]{"Genderswap", "Hoán Đổi Giới Tính"}),
            Map.entry("ghosts", new String[]{"Ghosts", "Linh Hồn / Ma Quái"}),
            Map.entry("girls-love", new String[]{"Girls' Love", "Bách Hợp"}),
            Map.entry("gore", new String[]{"Gore", "Máu Me"}),
            Map.entry("gyaru", new String[]{"Gyaru", "Gyaru"}),
            Map.entry("harem", new String[]{"Harem", "Harem (Nhiều Nữ)"}),
            Map.entry("historical", new String[]{"Historical", "Lịch Sử"}),
            Map.entry("horror", new String[]{"Horror", "Kinh Dị"}),
            Map.entry("incest", new String[]{"Incest", "Loạn Luân"}),
            Map.entry("isekai", new String[]{"Isekai", "Chuyển Sinh"}),
            Map.entry("loli", new String[]{"Loli", "Loli"}),
            Map.entry("long-strip", new String[]{"Long Strip", "Webtoon (Dải Dài)"}),
            Map.entry("mafia", new String[]{"Mafia", "Xã Hội Đen / Mafia"}),
            Map.entry("magic", new String[]{"Magic", "Ma Pháp"}),
            Map.entry("magical-girls", new String[]{"Magical Girls", "Thiếu Nữ Phép Thuật"}),
            Map.entry("martial-arts", new String[]{"Martial Arts", "Võ Thuật"}),
            Map.entry("mecha", new String[]{"Mecha", "Người Máy / Mecha"}),
            Map.entry("medical", new String[]{"Medical", "Y Học"}),
            Map.entry("military", new String[]{"Military", "Quân Sự"}),
            Map.entry("monster-girls", new String[]{"Monster Girls", "Nữ Quái"}),
            Map.entry("monsters", new String[]{"Monsters", "Quái Vật"}),
            Map.entry("music", new String[]{"Music", "Âm Nhạc"}),
            Map.entry("mystery", new String[]{"Mystery", "Bí Ẩn"}),
            Map.entry("ninja", new String[]{"Ninja", "Ninja"}),
            Map.entry("office-workers", new String[]{"Office Workers", "Công Sở"}),
            Map.entry("official-colored", new String[]{"Official Colored", "Màu Chính Thức"}),
            Map.entry("oneshot", new String[]{"Oneshot", "Oneshot"}),
            Map.entry("philosophical", new String[]{"Philosophical", "Triết Lý"}),
            Map.entry("police", new String[]{"Police", "Cảnh Sát / Trinh Thám"}),
            Map.entry("post-apocalyptic", new String[]{"Post-Apocalyptic", "Hậu Tận Thế"}),
            Map.entry("psychological", new String[]{"Psychological", "Tâm Lý"}),
            Map.entry("reincarnation", new String[]{"Reincarnation", "Trùng Sinh"}),
            Map.entry("reverse-harem", new String[]{"Reverse Harem", "Harem Ngược (Nhiều Nam)"}),
            Map.entry("romance", new String[]{"Romance", "Lãng Mạn"}),
            Map.entry("samurai", new String[]{"Samurai", "Samurai"}),
            Map.entry("school-life", new String[]{"School Life", "Học Đường"}),
            Map.entry("sci-fi", new String[]{"Sci-Fi", "Khoa Học Viễn Tưởng"}),
            Map.entry("self-published", new String[]{"Self-Published", "Tự Xuất Bản"}),
            Map.entry("sexual-violence", new String[]{"Sexual Violence", "Bạo Lực"}),
            Map.entry("shota", new String[]{"Shota", "Shota"}),
            Map.entry("slice-of-life", new String[]{"Slice of Life", "Đời Thường"}),
            Map.entry("sports", new String[]{"Sports", "Thể Thao"}),
            Map.entry("superhero", new String[]{"Superhero", "Siêu Anh Hùng"}),
            Map.entry("supernatural", new String[]{"Supernatural", "Siêu Nhiên"}),
            Map.entry("survival", new String[]{"Survival", "Sinh Tồn"}),
            Map.entry("thriller", new String[]{"Thriller", "Giật Gân"}),
            Map.entry("time-travel", new String[]{"Time Travel", "Du Hành Thời Gian"}),
            Map.entry("traditional-games", new String[]{"Traditional Games", "Trò Chơi Dân Gian"}),
            Map.entry("tragedy", new String[]{"Tragedy", "Bi Kịch"}),
            Map.entry("vampires", new String[]{"Vampires", "Ma Cà Rồng"}),
            Map.entry("video-games", new String[]{"Video Games", "Trò Chơi Điện Tử"}),
            Map.entry("villainess", new String[]{"Villainess", "Ác Nữ"}),
            Map.entry("virtual-reality", new String[]{"Virtual Reality", "Thực Tế Ảo"}),
            Map.entry("web-comic", new String[]{"Web Comic", "Webtoon"}),
            Map.entry("wuxia", new String[]{"Wuxia", "Kiếm Hiệp / Tiên Hiệp"}),
            Map.entry("zombies", new String[]{"Zombies", "Zombie / Xác Sống"})
    );

    @Override
    @Transactional
    public void run(String... args) {
        Role memberRole = initRoleIfNotFound("ROLE_MEMBER", "Quyen thanh vien doc truyen tieu chuan");
        Role adminRole = initRoleIfNotFound("ROLE_ADMIN", "Quyen quan tri vien he thong");
        initAdminUserIfNotFound(adminRole, memberRole);

        try {
            int backfilledChapters = storyRepository.backfillLatestChapterNames();
            int backfilledFeatured = storyRepository.backfillFeaturedAt();
            backfillBilingualCategories();
            if (backfilledChapters > 0 || backfilledFeatured > 0) {
                log.info("Da backfill du lieu khoi tao: {} truyen co chuong moi nhat, {} truyen de cu co thoi gian featuredAt",
                        backfilledChapters, backfilledFeatured);
            }
        } catch (Exception e) {
            log.warn("Bo qua backfill du lieu khoi tao neu bang chua san sang: {}", e.getMessage());
        }
    }

    private void backfillBilingualCategories() {
        List<Category> allCategories = categoryRepository.findAll();
        int updated = 0;
        for (Category cat : allCategories) {
            String slug = cat.getSlug();
            String[] bilingual = BILINGUAL_CATEGORIES.get(slug);
            if (bilingual != null) {
                cat.setName(bilingual[0]);
                cat.setNameVi(bilingual[1]);
                categoryRepository.save(cat);
                updated++;
            } else if (cat.getNameVi() == null || cat.getNameVi().isBlank()) {
                cat.setNameVi(cat.getName());
                categoryRepository.save(cat);
                updated++;
            }
        }
        if (updated > 0) {
            log.info("Da dong bo chuan hoa ten song ngu cho {} the loai trong database", updated);
        }
    }

    private Role initRoleIfNotFound(String name, String description) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    Role role = roleRepository.save(Role.builder()
                            .name(name)
                            .description(description)
                            .build());
                    log.info("Khoi tao role mac dinh: {}", name);
                    return role;
                });
    }

    private void initAdminUserIfNotFound(Role adminRole, Role memberRole) {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@struyen.com")
                    .passwordHash(passwordEncoder.encode("Admin@123456"))
                    .userStatus("ACTIVE")
                    .roles(Set.of(adminRole, memberRole))
                    .build();
            userRepository.save(admin);
            log.info("Khoi tao tai khoan Admin mac dinh: admin / Admin@123456");
        }
    }
}
