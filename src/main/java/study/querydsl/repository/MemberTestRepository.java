package study.querydsl.repository;


import static study.querydsl.entity.QMember.member;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.entity.Member;
import study.querydsl.repository.support.Querydsl4RepositorySupport;

@Repository
public class MemberTestRepository extends Querydsl4RepositorySupport {

    public MemberTestRepository(Class<?> domainClass) {
        super(Member.class);
    }

    public List<Member> basicSelect() {
        return select(member)
            .from(member)
            .fetch();
    }


    public List<Member> basicSelectFrom() {
        return selectFrom(member).fetch();
    }

    public Page<Member> searchPageByApplyPage(MemberSearchCondition condition,
        Pageable pageable) {
        return null;
    }


}

