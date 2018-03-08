package com.tierconnect.riot.appcore.services;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.appcore.dao.TokenDAO;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.servlet.TokenCacheHelper;

import javax.annotation.Generated;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Generated("com.tierconnect.riot.appgen.service.GenService")
public class TokenService extends TokenServiceBase 
{

    public void deleteAllBy(Group group) {
        TokenDAO tokenDAO = getTokenDAO();
        if (group.getTreeLevel() == 1) {
            tokenDAO.getDeleteQuery().execute();
        } else if (group.getTreeLevel() == 2) {
            QToken qToken = QToken.token;
            QUser qUser = QUser.user;
            List<Token> tokens = new HibernateQuery(tokenDAO.getSession()).from(qToken).innerJoin(qToken.user, qUser).where(qUser.group.parentLevel2.eq(group)).list(qToken);
            for (Token token : tokens) {
                tokenDAO.delete(token);
            }
            //tokenDAO.getDeleteQuery().where(QToken.token.user.group.parentLevel2.eq(group));
        }
    }

    public void deleteByUser(User user) {
        TokenDAO tokenDAO = getTokenDAO();
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QToken.token.user.id.eq(user.getId()));

        List<Token> tokens = tokenDAO.selectAll(be, null, null);
             for (Token token : tokens) {
                tokenDAO.delete(token);
            }

    }

    public void deactivateAllBy(Group group) {
        TokenDAO tokenDAO = getTokenDAO();
        QToken qToken = QToken.token;
        QUser qUser = QUser.user;
        List<Token> tokens = new ArrayList<>();
        if (group.getTreeLevel() == 1) {
            tokens = new HibernateQuery(tokenDAO.getSession()).from(qToken).where(qToken.tokenActive.eq(true)).list(qToken);
        } else if (group.getTreeLevel() == 2) {
            tokens = new HibernateQuery(tokenDAO.getSession()).from(qToken).innerJoin(qToken.user, qUser).where(qUser.group.parentLevel1.eq(group).and(qToken.tokenActive.eq(true))).list(qToken);
        }
        for (Token token : tokens) {
            token.setTokenActive(false);
            token.setTokenExpirationTime(new Date());
            tokenDAO.update(token);
            TokenCacheHelper.deActivate(token.getTokenString());
        }
    }
}

