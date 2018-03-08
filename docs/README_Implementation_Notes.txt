


General

1. 'Permissioning (or Authorization) System': The authorization system, resulting from a User's role, and that roles permissions on specific resources,
that detmermine if a user can create, read, write, delete, archive, execute, etc, a given resource.

2. The client (the web browser, running some dhtml framework, needs to be considered an untrusted user, and therefore anything returned in the
JSON web services needs to protected from a security perspsective. If a user does not have read permissions for a certian object or property, then that 
info should not appear in the returned JSON.

3. The essence of Core of appcore, the Users, Groups, Roles, Resources, is to provide a Rest Web services framework as automatically as 
possible supports the data authorisation 

JSON Serialization


1. Generally speaking, there will be some POJO service classes and methods (CRUD, List, and higher order biz logic) 
that will use Hibernate based DAOs to interact with the database to 
return either a single object or list of objects.

2. If using a JAX-RS framework, the these POJO services can be exposed vis JAX-RS annotations. 
This is in a nutshell the  whole point of JAX-RS, write POJO services and exposed them as RestFul web services magically via annotations.

3. It may be beneficial in some cases to role our own RESTFUl web services, in which case there might be a general http 
request dispatching servlet, which then dispatches to various
"Web Service" layer wrappers, which then call the POJO service classes.

4. In either case, there is the need to serialize java objects and Lists of objects into JSON.

Special considerations of appcore include:

a. The need to limit (i.e. not show) specific 'properties' (i.e. 'field' or 'columns') in the output data, per the permissioning system.
Generally speaking (maybe this should be a requirement), the permissioning system will have granular permissions of each main domain model class 
(e.g. User, Group, Part, PartType, Thing, ThingType, etc.) down to the property level (e.g. user.name, user.password, part.price, etc..
For example if a user requests a part, or list of parts, but does not have permission to read part.price, then this data field should not be
present in the JSON response stream.

b. The need to limit specific records (i.e. rows, or objects) in the response. In particular, a record's groups in comparison to the 
user's group. Most (if not all) top level domain objects must be associated with some group record, for this very reason. For example, 
a user who only
belongs to group "Department A" , inside some group "Michigan Truck Plant", should only be able to see part records associated with that group
"Department A" and its descendants.  In the multi-tenant scenario, some user belonging to group "Company A" should only be able to see users 
associated with group "Company A" its descendants. The user must not be allowed to see users who belong to "Company B". 

c. Typically JSON serializers have issues with circular references. The assumption is we will have these .... and so need to be able to handle this.

In a homegrown implementation of the web services it would probably be easy enough to build some reusable components that could effect the above.
The question is, for any given JAX-RS imp, which by it nature includes some serialization framework, is that serialization framework flexible 
or configurable enough to support
the above requirements. Some of them seem to offer quite a bit of customizable options, so this may be likely.

I can't quite remember now, but it seemed, for standard CRUD and LIST services, exposing these through some JAX-RS implementation yielded 
quite a bit of simple boiler plate code. I think at that point it became clear that standard CRUD and LIST could be written once for all classes, 
and so for this case it seemed to make sense to toss the JAX-RS framework in favor of a hand roled one. Once you get to the point of 
writing higher order biz logic, 
or things other than CRUD and Lists, is when a JAX-RS framework would really be used.

Things like input parameter validation, and other biz logic constraints (unique user name, etc) would then begin to complicate the hand 
rolled solution,
but then you could write a CRUD or List class specific to that case, which you could make the home grown services would support, 
or use a JAX-RS framework.


For deserializing, which would happen on insert or update operations, the permissioning aspect is not as difficult. This could be 
(in fact should be) done in the service methods themselves, after the serialization occurs.
The services methods should make sure they protect themselves against unauthorized inserts or updates to the system, be it from lack
visibility into the record from consideration of the groups, or from lack of permissions on a particular property.


 


















