package com.tierconnect.riot.api.database.utils;

import com.tierconnect.riot.api.database.base.alias.Alias;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by vealaro on 12/19/16.
 */
public class ListAliasForTest {

    private List<Alias> aliasList = new ArrayList<>();

    public ListAliasForTest(Alias... aliases) {
        aliasList.addAll(Arrays.asList(aliases));
    }

    public List<Alias> getAliasList() {
        return aliasList;
    }
}
