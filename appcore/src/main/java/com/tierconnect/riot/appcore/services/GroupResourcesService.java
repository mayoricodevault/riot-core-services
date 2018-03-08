package com.tierconnect.riot.appcore.services;


import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupResources;
import com.tierconnect.riot.appcore.utils.BlobUtils;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.UserException;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import javax.annotation.Generated;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Generated("com.tierconnect.riot.appgen.service.GenService")
public class GroupResourcesService extends GroupResourcesServiceBase 
{
    public GroupResources insert(GroupResources groupResources) {
        if (groupResources == null) {
            throw new UserException("Group Resource is Empty");
        }

        Long id = getGroupResourcesDAO().insert(groupResources);
        groupResources.setId(id);

        return groupResources;
    }

    public GroupResources update(GroupResources groupResources) {
        if (groupResources == null) {
            throw new UserException("Group Resource is Empty");
        }

        getGroupResourcesDAO().update(groupResources);
        return groupResources;
    }
    public Map<String, Object> uploadImageFile(String groupCode, String nameTemplate, Boolean setLastImage, MultipartFormDataInput input)  throws Exception
    {

        Map<String, Object> blobData = BlobUtils.getBlobMapByteArray(input, "file");

        Group group = GroupService.getInstance().getByCode(groupCode);
        List<GroupResources> groupResourcesList = new ArrayList<>(group.getGroupResources());
        GroupResources groupResources;

        String response = "No action executed";
        if (groupResourcesList.size() == 0) {
            groupResources = new GroupResources();

            if (nameTemplate != null) {
                groupResources.setImageTemplateName(nameTemplate);
            }else if((Integer) blobData.get("Size") > 0){
                groupResources.setImageTemplateName(null);
                groupResources.setImageIcon((byte[])blobData.get("Blob"));
            }
            groupResources.setGroup(group);
            this.insert(groupResources);

            response = "Inserted succesfully";

        }else {
            groupResources = groupResourcesList.get(0);
            //set last icon uploaded
            if (setLastImage) {
                if (groupResources.getImageIcon() != null) {
                    groupResources.setImageTemplateName(null);
                }else {
                    response = "Don't have icon";
                }
            }else if (nameTemplate != null) {
                //groupResources.setImageIcon(null);
                groupResources.setImageTemplateName(nameTemplate);
            }else if((Integer) blobData.get("Size") > 0) {
                groupResources.setImageTemplateName(null);
                groupResources.setImageIcon((byte[]) blobData.get("Blob"));
            }

            //save groupResource
            groupResources.setGroup(group);
            this.update(groupResources);

            response = "Updated succesfully";

        }


        Map<String, Object> result = new HashMap<>();
        result.put("message",response);
        return result;
    }
}

