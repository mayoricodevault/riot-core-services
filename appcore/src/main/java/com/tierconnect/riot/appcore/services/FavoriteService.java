package com.tierconnect.riot.appcore.services;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.controllers.FavoriteController;
import com.tierconnect.riot.appcore.controllers.FavoriteControllerBase;
import com.tierconnect.riot.appcore.entities.Favorite;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QFavorite;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.utils.EntityVisibility;
import com.tierconnect.riot.appcore.utils.GeneralVisibilityUtils;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.sdk.dao.MongoExecutionException;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;


import java.util.*;

public class FavoriteService extends FavoriteServiceBase
{

    public Map createFavorite (Map favoriteMap, EntityVisibility entityVisibility) {
        Map result = new HashMap<>();

        Long elementId = favoriteMap.get("elementId") != null ? Long.valueOf(favoriteMap.get("elementId").toString()) : null;
        String typeElement = favoriteMap.get("typeElement") != null ? favoriteMap.get("typeElement").toString() : null;
        String elementName = favoriteMap.get("elementName") != null ? favoriteMap.get("elementName").toString() : null;
        if (favoriteMap.get("elementGroupId") == null) {
            throw new UserException("Invalid Group: elementGroupId");
        }
        Group elementGroup = GroupService.getInstance().get(Long.valueOf(favoriteMap.get("elementGroupId").toString()));

        List<String> listTypes = Arrays.asList("thing", "thingtype","user","group","grouptype","role","shift","connection","zone",
                "zonetype","zonegroup","localmap","logicalreader","report","edgebox","smartContractParty","smartContractDefinition", "smartcontract","scheduledRule");
        if ( elementId == null ) {
            throw new UserException("Invalid input data: elementId");
        }
        if ( typeElement == null ) {
            throw new UserException("Invalid input data: typeElement");
        }
        if (!listTypes.contains(typeElement)){
            throw new UserException("Invalid type of element: "+typeElement);
        }
        if ( elementName == null ) {
            throw new UserException("Invalid input data: elementName");
        }
        if ( elementGroup == null ) {
            throw new UserException("Invalid Group: elementGroupId");
        }
        Subject subject = SecurityUtils.getSubject();
        User currentUser = (User) subject.getPrincipal();

        List<Favorite> listFavoritesTemp = listFavorites(currentUser.getId(), typeElement, "sequence:asc", entityVisibility, elementGroup.getId().longValue());
        Long sequence = 0L;
        if (listFavoritesTemp.isEmpty()){
            sequence = 1L;
        }else {
            Favorite tempFavorite = listFavoritesTemp.get(listFavoritesTemp.size() - 1);
            sequence = tempFavorite.getSequence() + 1L;
        }

        BooleanBuilder beAnd = new BooleanBuilder();
        beAnd = beAnd.and(QFavorite.favorite.user.eq(currentUser));
        beAnd = beAnd.and(QFavorite.favorite.typeElement.eq(typeElement));
        beAnd = beAnd.and(QFavorite.favorite.elementId.eq(elementId));
        List<Favorite> listElemet = FavoriteService.getInstance().listPaginated(beAnd, null, null);
        if (listElemet.isEmpty()) {

            Integer maxFavorite = ConfigurationService.getAsInteger(currentUser, "max_favoriteItem");
            BooleanBuilder be = new BooleanBuilder();
            be = be.and(QFavorite.favorite.user.eq(currentUser));
            be = be.and(QFavorite.favorite.typeElement.eq(typeElement));
            List<Favorite> listFavorites = FavoriteService.getInstance().listPaginated(be, null, null);
            if (listFavorites.size() + 1 > maxFavorite) {
                throw new UserException("It is not possible to add more favorites for this module, your maximum value configurated is: " + maxFavorite);
            } else {

                Favorite favorite = new Favorite();
                favorite.setElementId(elementId);
                favorite.setTypeElement(typeElement);
                favorite.setElementName(elementName);
                favorite.setGroup(elementGroup);
                favorite.setUser(currentUser);
                favorite.setDate(new Date().getTime());
                favorite.setStatus("ADDED");
                favorite.setSequence(sequence);
                FavoriteService.getInstance().insert(favorite);
                result = favorite.publicMap();


            }
        }else {
            Favorite favorite = listElemet.get(0);
            favorite.setDate(new Date().getTime());
            FavoriteService.getInstance().update(favorite);
            result = favorite.publicMap();
        }
        return result;

    }

    public List<Favorite> listFavorites(Long userId, String typeElement, String order, EntityVisibility entityVisibility, Long visibilityGroupId) {
            BooleanBuilder predicate = new BooleanBuilder();
        if (visibilityGroupId != null && entityVisibility != null) {
            Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Favorite.class.getCanonicalName(), visibilityGroupId);
            if (visibilityGroup.getId().longValue() == GroupService.getInstance().getRootGroup().getId().longValue()) {
                predicate = predicate.and( QueryUtils.buildSearch( QFavorite.favorite, "group.id=" + Long.toString(visibilityGroup.getId().longValue()) ) );
            } else {
                predicate = predicate.and( GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, QFavorite.favorite,  visibilityGroup, "false", "true") );
            }
        }
        predicate = predicate.and(QFavorite.favorite.typeElement.eq(typeElement));
        predicate=predicate.and(QFavorite.favorite.user.id.eq(userId));
        return FavoriteService.getInstance().listPaginated(predicate, null, order);
    }

    public List<Map<String, Object>> addFavoritesToList(List<Map<String, Object>> list, Long userId, String typeElement) {
        String id = "id";
        List<Favorite> listFavorites = listFavorites(userId, typeElement, null, null, null);
        if (typeElement.equals("thing") || typeElement.equals("smartcontract")) {
            id = "_id";
        }
        for (Map map : list) {
            Long elementId = (Long) map.get(id);
            for (Favorite favorite : listFavorites) {
                Long favId = favorite.getElementId();
                if (elementId.compareTo(favId) == 0) {
                    map.put("favoriteId", favorite.getId());
                    break;
                }
            }
            if (map.containsKey("children")) {
                List<Map<String, Object>> listChildren = (List<Map<String, Object>>) map.get("children");
                processChildren(listChildren, listFavorites, typeElement);
            }
        }
        return list;
    }

    public List<Map<String, Object>> addFavoritesForGroup(List<Map<String, Object>> list, Long userId, String typeElement){
        List<Favorite> listFavorites = listFavorites(userId, typeElement, null, null, null);
        processChildren(list, listFavorites, typeElement);
        return list;
    }

    public List<Map<String, Object>> processChildren(List<Map<String, Object>> children, List<Favorite> listFavorites, String typeElement){
        if (children.size() > 0 && children.size() != 1){
            for (Map map:children){
                Long elementId = null;
                if (typeElement.equals("thing")){
                    elementId = (Long) map.get("_id");
                }else {
                    elementId = (Long) map.get("id");
                }
                for (Favorite favorite: listFavorites){
                    Long favId = favorite.getElementId();
                    if (elementId.compareTo(favId) == 0){
                        map.put("favoriteId",favorite.getId());
                        break;
                    }
                }
                List<Map<String, Object>> listChildren = (List)map.get("children");
                if (listChildren != null && listChildren.size() > 0) {
                    processChildren(listChildren, listFavorites, typeElement);
                }
            }
        }else{
            if (children.size() == 1){
                Map map= children.get(0);
                Long elementId = null;
                if (typeElement.equals("thing")){
                    elementId = (Long) map.get("_id");
                }else {
                    elementId = (Long) map.get("id");
                }

                for (Favorite favorite: listFavorites){
                    Long favId = favorite.getElementId();
                    if (elementId.compareTo(favId) == 0){
                        map.put("favoriteId",favorite.getId());
                        break;
                    }
                }
                List<Map<String, Object>> listChildren = (List)map.get("children");
                if (listChildren != null && listChildren.size() > 0){
                    processChildren(listChildren, listFavorites, typeElement);
                }
            }
        }
        return children;
    }

    public List<Map<String, Object>> updateSequence (List<Long> favoriteList){
        List<Map<String, Object>> favorites = new ArrayList<>();
        Long i = 1L;
        for(Long id: favoriteList){
            Favorite favorite = FavoriteService.getInstance().get(id);
            if (favorite != null){
                favorite.setSequence(i);
                FavoriteService.getInstance().update(favorite);
                favorites.add(favorite.publicMap());
                i = i +1;
            }
        }
        return favorites;
    }

}

