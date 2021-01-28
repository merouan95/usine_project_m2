package fr.noel.project.service.impl;



import fr.noel.project.dto.*;
import fr.noel.project.entities.*;
import fr.noel.project.repositories.AppUserRepository;
import fr.noel.project.repositories.CategoriesRepository;
import fr.noel.project.repositories.CompetenceRepository;
import fr.noel.project.repositories.RoleRepository;
import fr.noel.project.service.TacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

@Service
@Transactional
public class AppUserServiceImpl implements UserDetailsService {

    @Autowired
    private AppUserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private CompetenceRepository competenceRepository;
    @Autowired
    private CategoriesRepository categoriesRepository;
    @Autowired
    private TacheService tacheService;

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {

        final AppUser user = this.userRepository.findByEmail(s);
        if (user == null) {
            throw new UsernameNotFoundException("email : " + s + " is not found");
        }
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority(user.getRole().getName()));

        return new UserPrincipal(user, authorities);
    }

    public AppUser findByEmail(String email) {
        return this.userRepository.findByEmail(email);
    }

    public AppUser saveUser(UserDto dto) throws Exception {
        if (this.findByEmail(dto.getEmail()) != null) {
            throw new Exception("EMAIL ALREADY EXISTS");
        }
        AppUser appUser = new AppUser();
        appUser.setEmail(dto.getEmail());
        appUser.setName(dto.getName());
        appUser.setCreatedBy(dto.getCreatedBy());
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        appUser.setPassword(encoder.encode(dto.getPassword()));
        appUser.setRole(this.roleRepository.findByName(dto.getRole()));
        appUser.setCreationDate(Instant.now());

        final List<CompetenceDto> userComp = dto.getCompetences();
        if (userComp != null && userComp.size() > 0) {
            final List<Long> ids = userComp.stream().map(CompetenceDto::getId).collect(Collectors.toList());
            final List<Competence> listEntityComp = this.competenceRepository.findByIdIn(ids);
            appUser.setCompetences(listEntityComp);
        }

        return this.userRepository.save(appUser);
    }

    public Long findIdByEmail(String email) {
        return this.userRepository.findIdByEmail(email);
    }


    public void initApp() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        Role adminRole = new Role();
        adminRole.setName(RoleName.ADMIN.name());

        Role userRole = new Role();
        userRole.setName(RoleName.USER.name());


        adminRole = this.roleRepository.save(adminRole);
        userRole = this.roleRepository.save(userRole);

        AppUser admin1 = new AppUser();
        admin1.setName("PAPA NOEL");
        admin1.setEmail("papa@admin.fr");
        admin1.setPassword(encoder.encode("123456"));
        admin1.setRole(adminRole);
        admin1.setCreationDate(Instant.now());
        admin1 = this.userRepository.save(admin1);

        AppUser admin2 = new AppUser();
        admin2.setName("MAMA NOEL");
        admin2.setEmail("mere@admin.fr");
        admin2.setPassword(encoder.encode("123456"));
        admin2.setRole(adminRole);
        admin2.setCreationDate(Instant.now());
        admin2 = this.userRepository.save(admin2);


        AppUser user1 = new AppUser();
        user1.setName("USER 1");
        user1.setEmail("user1@user.fr");
        user1.setPassword(encoder.encode("123456"));
        user1.setRole(userRole);
        user1.setCreationDate(Instant.now());
        user1.setCreatedBy(admin1.getId());
        user1 = this.userRepository.save(user1);

        AppUser user2 = new AppUser();
        user2.setName("USER 2");
        user2.setEmail("user2@user.fr");
        user2.setPassword(encoder.encode("123456"));
        user2.setRole(userRole);
        user2.setCreationDate(Instant.now());
        user2.setCreatedBy(admin1.getId());
        user2 = this.userRepository.save(user2);


        AppUser user3 = new AppUser();
        user3.setName("USER 3");
        user3.setEmail("user3@admin.fr");
        user3.setPassword(encoder.encode("123456"));
        user3.setRole(userRole);
        user3.setCreationDate(Instant.now());
        user2.setCreatedBy(admin2.getId());
        user3 = this.userRepository.save(user3);

    }

    public void initCompetence() {
        Competence competence1 = new Competence();
        Competence competence2 = new Competence();
        Competence competence3 = new Competence();
        Competence competence4 = new Competence();
        Competence competence5 = new Competence();
        competence1.setName("CARPENTRY");
        competence2.setName("PLOMBERY");
        competence3.setName("ELECTRONIC");
        competence4.setName("AUTONOMIE");
        competence5.setName("POLYVALENCE");
        final List<Competence> competences = Arrays.asList(competence1, competence2, competence3, competence4, competence5);
        this.competenceRepository.saveAll(competences);
    }

    public void initCategorie() {
        Categorie categorie1 = new Categorie();
        Categorie categorie2 = new Categorie();
        Categorie categorie3 = new Categorie();

        categorie1.setName("Sport");
        categorie2.setName("Reflexion");
        categorie3.setName("Jouet pour fille");

        final List<Categorie> categorieList = Arrays.asList(categorie1, categorie2, categorie3);
        this.categoriesRepository.saveAll(categorieList);


    }


    public boolean isUserAvailable(AppUser user) {
        final List<Tache> taches = this.tacheService.allTachesByUser(user.getId());
        if (taches != null && taches.size() > 0) {
            BinaryOperator<Tache> maxTacheAccumulator = (t1, t2) -> {
                final long duration1 = t1.getDateAffectation().until(t1.getDateFin(), ChronoUnit.MINUTES);
                final long duration2 = t2.getDateAffectation().until(t2.getDateFin(), ChronoUnit.MINUTES);
                if (duration1 > duration2) {
                    return t1;
                }else {
                    return t2;
                }
            };
            final Tache maxDuree = taches.stream().reduce(taches.get(0), maxTacheAccumulator);
            final int maxDureeMinute = maxDuree.getJeux().getDuree();
            Duration duration = Duration.ofMinutes(maxDureeMinute);
            final Instant dateFinTache = maxDuree.getDateAffectation().plus(duration);
            if(dateFinTache.isBefore(Instant.now())){
                return true;
            }
            else {
                return false;
            }
        }
        return true;
    }


    public ResponseDto allUsers(Long adminId) {
        Map<String, List<UserDto>> content = new HashMap<>();
        try {
            final List<AppUser> byCreatedBy = this.userRepository.findByCreatedBy(adminId);
            final Map<Boolean, List<AppUser>> allUsers = byCreatedBy.stream().collect(Collectors.partitioningBy(this::isUserAvailable));
            final List<UserDto> availableUsers = allUsers.get(true).stream().map(UserDto::toUserDto).collect(Collectors.toList());
            final List<UserDto> notAvailableUsers = allUsers.get(false).stream().map(UserDto::toUserDto).collect(Collectors.toList());

            content.put("availaible", availableUsers);
            content.put("notAvailaible", notAvailableUsers);
            return new ContentResponseDto(true, "OK", content);
        } catch (Exception e) {
            return new ResponseDto(false, e.getMessage());
        }
    }

    public Map<String, Long> stateUsers(Long adminId) {
        Map<String, Long> content = new HashMap<>();
        final List<AppUser> byCreatedBy = this.userRepository.findByCreatedBy(adminId);
        final Map<Boolean, List<AppUser>> allUsers = byCreatedBy.stream().collect(Collectors.partitioningBy(this::isUserAvailable));
        final List<UserDto> availableUsers = allUsers.get(true).stream().map(UserDto::toUserDto).collect(Collectors.toList());
        final List<UserDto> notAvailableUsers = allUsers.get(false).stream().map(UserDto::toUserDto).collect(Collectors.toList());
        content.put("availaibleUsers", Long.valueOf(availableUsers.size()));
        content.put("notavailaibleUsers", Long.valueOf(notAvailableUsers.size()));
        return content;
    }

    public ResponseDto findOneById(Long id){
        final Optional<AppUser> byId = this.userRepository.findById(id);
        if(byId.isPresent()){
            final AppUser appUser = byId.get();
            return new ContentResponseDto(true,"OK",UserDto.toUserDto(appUser));
        }
        else{
            return new ResponseDto(false,"NOT FOUND");
        }
    }
}
