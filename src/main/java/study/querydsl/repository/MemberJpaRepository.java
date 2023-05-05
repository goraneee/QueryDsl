package study.querydsl.repository;

import static org.springframework.util.StringUtils.hasText;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;

@Repository
public class MemberJpaRepository {


    private final EntityManager em;
    private final JPAQueryFactory queryFactory;
    private final QMember member = QMember.member;
    private final QTeam team = QTeam.team;

    


    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll() {
        return em.createQuery("SELECT m FROM Member m", Member.class).getResultList();
    }

    public List<Member> findByUsername(String username) {
        return em.createQuery("SELECT m FROM Member m WHERE m.username = :username", Member.class)
            .setParameter("username", username)
            .getResultList();
    }

    public List<Member> findAll_Querydsl() {
        return queryFactory.selectFrom(member).fetch();
    }

    public List<Member> findByUsername_Querydsl(String username) {
        return queryFactory.selectFrom(member)
            .where(member.username.eq(username))
            .fetch();
    }


    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();
        if(hasText(condition.getUsername())){
            builder.and(member.username.eq(condition.getUsername()));
        }
        if(hasText(condition.getTeamName())){
            builder.and(member.team.name.eq(condition.getTeamName()));
        }
        return queryFactory.select(
        new QMemberTeamDto(member.id, member.username, member.age, team.id, team.name))
            .from(member)
            .leftJoin(member.team, team)
            .where(builder)
            .fetch();
    }


    /*
    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();
        if (hasText(condition.getUsername())) {
            builder.and(member.username.eq(condition.getUsername()));
        }
        if (hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }

        return queryFactory
            .select(new QMemberTeamDto(
                member.id,
                member.username,
                member.age,
                team.id,
                team.name))
            .from(member)
            .leftJoin(member.team, team)
            .where(builder)
            .fetch();
    }*/


}