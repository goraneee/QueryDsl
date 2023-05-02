package study.querydsl.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;

@Repository
public class MemberJpaRepository {


    private final EntityManager em;
    private final JPAQueryFactory queryFactory;
    private final QMember member = QMember.member;


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

}