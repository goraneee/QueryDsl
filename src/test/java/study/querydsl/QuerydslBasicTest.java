package study.querydsl;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;
import study.querydsl.repository.MemberJpaRepository;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;

    @PersistenceUnit
    EntityManagerFactory emf;

    JPAQueryFactory queryFactory;

    MemberJpaRepository memberJpaRepository = new MemberJpaRepository(em);


    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }


    @Test
    public void startQueryDSL2() {
        QMember m = new QMember("m");
        Member findMember = queryFactory.select(m)
            .from(m)
            .where(m.username.eq("member1"))
            .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }


    @Test
    public void startQueryDSL3() {
        Member findMember = queryFactory.select(member)
            .from(member)
            .where(member.username.eq("member1"))
            .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {
        Member findMember = queryFactory
            .selectFrom(member)
            .where(member.username.eq("member1")
                , member.age.eq(10))
            .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }


    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory.selectFrom(member)
            .where(member.age.eq(100))
            .orderBy(member.age.desc(), member.username.asc().nullsLast())
            .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();

    }


    @Test
    public void paging() {

        QueryResults<Member> queryResult = queryFactory
            .selectFrom(member)
            .orderBy(member.username.desc())
            .offset(1)
            .limit(2)
            .fetchResults();
        System.out.println(queryResult.toString());
        assertThat(queryResult.getTotal()).isEqualTo(4);
        assertThat(queryResult.getLimit()).isEqualTo(2);
        assertThat(queryResult.getOffset()).isEqualTo(1);
        assertThat(queryResult.getResults().size()).isEqualTo(2);
    }

    @Test
    public void theta_join() throws Exception {

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
            .select(member)
            .from(member, team)
            .where(member.username.eq(team.name))
            .fetch();

        assertThat(result).extracting("username")
            .containsExactly("teamA", "teamB");
    }

    @Test
    public void fetchJoinNo() throws Exception {

        em.flush();
        em.clear();

        Member findMember = queryFactory
            .selectFrom(member)
            .where(member.username.eq("member1"))
            .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();

    }

    @Test
    public void fetchJoinUse() throws Exception {

        em.flush();
        em.clear();

        Member findMember = queryFactory
            .selectFrom(member)
            .where(member.username.eq("member1"))
            .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();

    }

    @Test
    public void subQuery() throws Exception {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.eq(JPAExpressions
                .select(memberSub.age.max())
                .from(memberSub)))
            .fetch();

        assertThat(result).extracting("age")
            .containsExactly(40);

        // 별칭이 다를 떄
        List<UserDto> result4 = queryFactory
            .select(Projections.fields(UserDto.class,
                member.username.as("name"),
                ExpressionUtils.as(
                    JPAExpressions
                        .select(memberSub.age.max())
                        .from(memberSub), "age")
            ))
            .from(member)
            .fetch();
    }

    @Test
    public void subQueryGoe() throws Exception {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.goe(JPAExpressions
                .select(memberSub.age.avg())
                .from(memberSub)))
            .fetch();

        assertThat(result).extracting("age")
            .containsExactly(30, 40);
    }

    @Test
    public void subQueryIn() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.in(
                JPAExpressions
                    .select(memberSub.age)
                    .from(memberSub)
                    .where(memberSub.age.gt(10))))
            .fetch();

        assertThat(result).extracting("age")
            .containsExactly(20, 30, 40);
    }

    @Test
    void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");
        List<Tuple> fetch = queryFactory.select(member.username,
                JPAExpressions.select(memberSub.age.avg())
                    .from(memberSub)
            ).from(member)
            .fetch();

        for (Tuple tuple : fetch) {
            System.out.println("username = " + tuple.get(member.username));
            System.out.println(
                "age = " + tuple.get(JPAExpressions
                    .select(memberSub.age.avg())
                    .from(memberSub)));
        }
    }

    @Test
    void orderByCase() {

        // 단순 조건
        List<String> result1 = queryFactory.select(
                member.age
                    .when(10).then("열살")
                    .when(20).then("스무살")
                    .otherwise("기타"))
            .from(member)
            .fetch();

        // 복잡 조건
        List<String> result2 = queryFactory.select(
                new CaseBuilder()
                    .when(member.age.between(0, 20)).then("0~20살")
                    .when(member.age.between(21, 30)).then("21~30살")
                    .otherwise("기타"))
            .from(member)
            .fetch();

        // orderBy에서 Case문 함께 사용하기
        NumberExpression<Integer> rankPath = new CaseBuilder()
            .when(member.age.between(0, 20)).then(2)
            .when(member.age.between(21, 30)).then(1)
            .otherwise(3);
        List<Tuple> result3 = queryFactory
            .select(member.username, member.age, rankPath)
            .from(member)
            .orderBy(rankPath.desc())
            .fetch();
        for (Tuple tuple : result3) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            Integer rank = tuple.get(rankPath);
            System.out.println("username = " + username + " age = " + age + " rank = " + rank);
        }
    }

    @Test
    void constantAndConcat() {
        Tuple result = queryFactory.select(member.username, Expressions.constant("A"))
            .from(member)
            .from(member)
            .fetchFirst();

        String result2 = queryFactory
            .select(member.username.concat("_").concat(member.age.stringValue()))
            .from(member)
            .where(member.username.eq("member1"))
            .fetchOne();
    }

    // 순수 JPA 에서 DTO 조회 코드
    void test00() {
        List<MemberDto> result = em.createQuery(
            "SELECT new study.querydsl.dto.MemberDto(m.username, m.age) FROM MEMBER m",
            MemberDto.class).getResultList();
    }

    @Test
    void test01() {
        // 프로퍼티에 접근
        List<MemberDto> result2 = queryFactory.select(
                Projections.bean(MemberDto.class, member.username, member.age))
            .from(member)
            .fetch();
        for (MemberDto member : result2) {
            System.out.println(member.toString());
        }
        System.out.println();

        // 필드에 직접 접근
        List<MemberDto> result3 = queryFactory.select(Projections
                .fields(MemberDto.class, member.username, member.age))
            .from(member)
            .fetch();
        for (MemberDto member : result2) {
            System.out.println(member.toString());
        }
        System.out.println();
    }

    // 별칭이 다를 떄
    void test02() {
        QMember memberSub = new QMember("memberSub");
        List<UserDto> fetch = queryFactory
            .select(Projections.fields(UserDto.class,
                member.username.as("name"),
                ExpressionUtils.as(
                    JPAExpressions
                        .select(memberSub.age.max())
                        .from(memberSub), "age")
            ))
            .from(member)
            .fetch();
    }

    // 생성자 사용
    void test03() {
        List<MemberDto> result = queryFactory
            .select(Projections.constructor(MemberDto.class,
                member.username,
                member.age))
            .from(member)
            .fetch();
    }

    // QueryProjection 활용
    void test04() {
        List<MemberDto> result = queryFactory
            .select(new QMemberDto(
                member.username,
                member.age))
            .from(member)
            .fetch();

        List<String> result0 = queryFactory
            .select(member.username).distinct()
            .from(member)
            .fetch();
    }

    // 동적 쿼리 - BooleanBuilder
    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }
        return queryFactory.selectFrom(member)
            .where(builder)
            .fetch();
    }

    @Test
    public void dynamicQuery_booleanBuilder() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;
        List<Member> result = searchMember1(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    // 동적 쿼리 - where 다중 파라미터
    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory.selectFrom(member)
            .where(usernameEq(usernameCond), ageEq(ageCond))
            .fetch();
    }

    @Test
    public void dynamicQuery_whereParam() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;
        List<Member> result = searchMember2(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    // 수정 삭제 벌크 연산
    void test05() {
        long count = queryFactory.update(member)
            .set(member.username, "비회원")
            .where(member.age.lt(28))
            .execute();

        long count1 = queryFactory.update(member)
            .set(member.age, member.age.add(1))
            .execute();

        long count2 = queryFactory.delete(member)
            .where(member.age.gt(18))
            .execute();
    }

    // sql_function 호출하기
    void test06() {
        String result = queryFactory
            .select(
                Expressions.stringTemplate("function('replace', {0}, {1}, {2})", member.username,
                    "member", "M"))
            .from(member)
            .fetchFirst();
        String result1 = queryFactory
            .select(member.username)
            .from(member)
            .where(member.username
                .eq(Expressions.stringTemplate("function('lower', {0})", member.username)))
            .fetchFirst();
    }

    // 동적쿼리 - Builder 사용
    @Test
    void searchTest(){
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");
        List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);
        assertThat(result).extracting("username").containsExactly("member4");
    }



}
