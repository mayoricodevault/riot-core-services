package com.tierconnect.riot.appcore.services;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.hibernate.StaleObjectStateException;
import org.hibernate.exception.ConstraintViolationException;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : aruiz
 * @date : 4/4/17 5:33 PM
 * @version:
 */
public class RecentService extends RecentServiceBase {

    static Logger logger = Logger.getLogger(RecentService.class);

    public Map createRecent (Map favoriteMap){
        Map result = new HashMap<>();

        Long elementId = favoriteMap.get("elementId") != null ? Long.valueOf(favoriteMap.get("elementId").toString()) : null;
        String typeElement = favoriteMap.get("typeElement") != null ? favoriteMap.get("typeElement").toString() : null;
        String elementName = favoriteMap.get("elementName") != null ? favoriteMap.get("elementName").toString() : null;
        Long elementGroupId = favoriteMap.get("elementGroupId") != null ? Long.valueOf(favoriteMap.get("elementGroupId").toString()) : null;
        if ( elementId == null ) {
            throw new UserException("Invalid input data: elementId");
        }
        if ( typeElement == null ) {
            throw new UserException("Invalid input data: typeElement");
        }
        if ( elementName == null ) {
            throw new UserException("Invalid input data: elementName");
        }
        Subject subject = SecurityUtils.getSubject();
        User currentUser = (User) subject.getPrincipal();

        Recent recent = new Recent();
        recent.setElementId(elementId);
        recent.setTypeElement(typeElement);
        recent.setElementName(elementName);
        recent.setElementGroupId(elementGroupId);
        recent.setUser(currentUser);
        recent.setDate(new Date().getTime());
        RecentService.getInstance().insert(recent);
        result = recent.publicMap();
        return result;
    }

    public List<Recent> listRecents(Long userId, String typeElement, String order){

        BooleanBuilder predicate = new BooleanBuilder();
        predicate = predicate.and(QRecent.recent.typeElement.eq(typeElement));
        predicate=predicate.and(QRecent.recent.user.id.eq(userId));
        return RecentService.getInstance().listPaginated(predicate, null, order);
    }

    public void recentThings(Map<String, Object> recentMap){

        Map temporalMap = (HashMap)recentMap.get("thing");
        Subject subject = SecurityUtils.getSubject();
        User currentUser = (User) subject.getPrincipal();
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QRecent.recent.user.eq(currentUser));
        be = be.and(QRecent.recent.typeElement.eq("thing"));
        be = be.and(QRecent.recent.elementId.eq(Long.valueOf(temporalMap.get("id").toString())));
        List<Recent> recents = RecentService.getInstance().listPaginated(be, null, null );
        if (recents.isEmpty()) {
            Recent recent = new Recent();
            recent.setDate(new Date().getTime());
            recent.setElementId(Long.valueOf(temporalMap.get("id").toString()));
            recent.setElementName(temporalMap.get("name").toString());
            recent.setUser(currentUser);
            if (temporalMap.get("groupId")!= null){
                recent.setElementGroupId(Long.valueOf(temporalMap.get("groupId").toString()));
            }
            recent.setTypeElement("thing");
            RecentService.getInstance().insert(recent);
        }else{
            for (Recent recentUpdate : recents){
                recentUpdate.setElementName(temporalMap.get("name").toString());
                recentUpdate.setDate( new Date().getTime());
                RecentService.getInstance().update(recentUpdate);
            }
        }

        Integer maxRecent = ConfigurationService.getAsInteger(currentUser,"max_recentItem");
        BooleanBuilder beAnd = new BooleanBuilder();
        beAnd = beAnd.and(QRecent.recent.user.eq(currentUser));
        beAnd = beAnd.and(QRecent.recent.typeElement.eq("thing"));
        List<Recent> allRecents = RecentService.getInstance().listPaginated(beAnd, null, "date:desc");
        if (allRecents.size() > maxRecent){
            for (int i = maxRecent; i < allRecents.size(); i++){
                RecentService.getInstance().delete(allRecents.get(i));
            }
        }
    }

    public void updateName(Long id, String name, String typeElement){
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QRecent.recent.elementId.eq(id));
        be = be.and(QRecent.recent.typeElement.eq(typeElement));

        List<Recent> listRecent = RecentService.getInstance().listPaginated(be, null, null);
        for (Recent recent : listRecent){
            recent.setElementName(name);
            RecentService.getInstance().update(recent);
        }
    }

    @Override
    public Recent insert(Recent recent) {
        Long id = getRecentDAO().insert(recent);
        recent.setId(id);
        return recent;
    }

    @Override
    public Recent update(Recent recent) {
        getRecentDAO().update( recent );
        return super.update(recent);
    }

    @Override
    public void delete(Recent recent) {
        getRecentDAO().delete(recent);
    }

    public void insertRecent(Long id, String name, String typeElement, Group group){
        try{
            insertRecent(id, name, typeElement, group.getId());
        }catch (ConstraintViolationException | StaleObjectStateException e){
            logger.warn("Recent Element was already created or modified with a superior version");
        }
    }

    public void insertRecent(Long id, String name, String typeElement, Long groupId){
        Subject subject = SecurityUtils.getSubject();
        User currentUser = (User) subject.getPrincipal();
        List <Recent> recents = getRecentsWithSpecificElementId(currentUser, typeElement, id);

        if (recents.isEmpty()) {
            deleteOverflowRecents(typeElement, currentUser);
            Recent recent = new Recent();
            recent.setDate(new Date().getTime());
            recent.setElementId(id);
            recent.setElementName(name);
            recent.setUser(currentUser);
            recent.setTypeElement(typeElement);
            recent.setElementGroupId(groupId);
            RecentService.getInstance().insert(recent);
        }else{
            for (Recent recentUpdate : recents){
                recentUpdate.setElementName(name);
                recentUpdate.setDate( new Date().getTime());
                RecentService.getInstance().update(recentUpdate);
            }
        }
    }

    private void deleteOverflowRecents(String typeElement, User currentUser) {
        //TODO: this algorithm should be improved for concurrency, produces low throughput
        Integer maxRecent = ConfigurationService.getAsInteger(currentUser,"max_recentItem");
        List<Recent> allRecents = getRecents(currentUser, typeElement);
        if (allRecents.size() + 1 > maxRecent){
            for (int i = maxRecent; i < allRecents.size(); i++){
                RecentService.getInstance().delete(allRecents.get(i));
            }
        }
    }

    public List<Recent> getRecentsWithSpecificElementId(User user, String typeElement, Long elementId){
        HibernateQuery query = new HibernateQuery(HibernateSessionFactory.getInstance().getCurrentSession());
        QRecent recent = QRecent.recent;
        return query.from(recent)
                .where(recent.user.eq(user)
                        .and(recent.typeElement.eq(typeElement))
                        .and(recent.elementId.eq(elementId)))
                .setCacheable(true)
                .list(recent);
    }

    public List<Recent> getRecents(User user, String typeElement){
        HibernateQuery query = new HibernateQuery(HibernateSessionFactory.getInstance().getCurrentSession());
        QRecent recent = QRecent.recent;
        return query.from(recent)
                .where(recent.user.eq(user)
                        .and(recent.typeElement.eq(typeElement)))
                .orderBy(recent.date.desc())
                .setCacheable(true)
                .list(recent);
    }

    public void deleteRecent(Long id, String typeElement){
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QRecent.recent.typeElement.eq(typeElement));
        be = be.and(QRecent.recent.elementId.eq(id));

        List <Recent> recents = RecentService.getInstance().listPaginated(be, null, null);
        for(Recent recent : recents){
            RecentService.getInstance().delete(recent);
        }

    }

    public void deleteRecent(Long id, String typeElement, User curentUser){
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QRecent.recent.typeElement.eq(typeElement));
        be = be.and(QRecent.recent.elementId.eq(id));

        List <Recent> recents = RecentService.getInstance().listPaginated(be, null, null);
        for(Recent recent : recents){
            getRecentDAO().delete( recent );
            deleteFavorite(recent, curentUser);
        }

    }

    public void deleteRecentByUser(User currentUser){
        BooleanBuilder be = new BooleanBuilder();

        be = be.and(QRecent.recent.user.id.eq(currentUser.getId()));

        List <Recent> recents = RecentService.getInstance().listPaginated(be, null, null);
        for(Recent recent : recents){
            getRecentDAO().delete( recent );
            deleteFavorite(recent, currentUser);
        }

    }

    public void deleteFavorite( Recent recent , User currentUser) {
        String typeElement = "recent";
        Long elementId = recent.getId();

        BooleanBuilder beAnd = new BooleanBuilder();
        beAnd = beAnd.and(QFavorite.favorite.typeElement.eq(typeElement));
        beAnd = beAnd.and(QFavorite.favorite.elementId.eq(elementId));
        beAnd = beAnd.and(QFavorite.favorite.user.eq(currentUser));

        List<Favorite> favorites = FavoriteService.getInstance().listPaginated(beAnd, null, null);
        if (!favorites.isEmpty()){
            FavoriteService.getInstance().delete(favorites.get(0));
        }
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QFavorite.favorite.typeElement.eq(typeElement));
        be = be.and(QFavorite.favorite.elementId.eq(elementId));
        List <Favorite> listFavorite = FavoriteService.getInstance().listPaginated(be,null, null);
        for (Favorite favorite1: listFavorite){
            favorite1.setStatus("DELETED");
            FavoriteService.getInstance().update(favorite1);
        }
    }
}
