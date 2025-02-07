package com.twinsoft.convertigo.engine.admin.services.tree;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.FolderType;

@ServiceDefinition(
		name = "Get",
		roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW },
		parameters = {},
		returnValue = ""
		)
public class Get extends JSonService {
	static Pattern parseQName = Pattern.compile("(.*?)(?::(\\w+?))?");

	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var ids = request.getParameter("ids");
		if (ids != null) {
			var jids = new JSONArray(ids);
			for (int i = 0; i < jids.length(); i++) {
				var id = jids.getString(i);
				response.put(id, getChildren(id));
			}
			return;
		}
		var id = request.getParameter("id");

		response.put("children", getChildren(id));

		response.put("id", id);
	}

	private JSONArray getChildren(String id) throws Exception {
		var children = new JSONArray();
		if (id == null) {
			for (String projectName: Engine.theApp.databaseObjectsManager.getAllProjectNamesList(true)) {
				var dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(projectName);
				children.put(getNode(dbo, true));
			}
		} else if (id.contains("/")) {
			children = getFileChildren(id);
		} else {
			var reg = parseQName.matcher(id);
			reg.matches();
			var ft = FolderType.parse(reg.group(2));
			var qname = ft == null ? id : reg.group(1);
			var dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
			children = getChildren(dbo, ft, true);
		}
		return children;
	}

	private JSONObject getNode(DatabaseObject dbo, boolean full) throws Exception {
		var qname = dbo.getFullQName();
		var obj = new JSONObject();
		obj.put("label", dbo.toString());
		obj.put("icon", "database_objects.GetIcon?large=true&className=" + dbo.getClass().getCanonicalName());
		obj.put("id", qname);
		if (!full) {
			obj.put("children", dbo.hasDatabaseObjectChildren());
		} else {
			var jChildren = getChildren(dbo, null, false);
			obj.put("children", jChildren);
			if (dbo instanceof Project) {
				var o = new JSONObject();
				o.put("label", "Files");
				o.put("icon", "folder");
				o.put("id", qname + '/');
				o.put("children", true);
				jChildren.put(o);
			}
		}
		return obj;
	}

	private JSONArray getChildren(DatabaseObject dbo, FolderType ft, boolean full) throws Exception {
		var qname = dbo.getFullQName();
		var children = dbo.getDatabaseObjectChildren();
		var jChildren = new JSONArray();
		var map = new HashMap<FolderType, JSONObject>();
		for (var child: children) {
			var cft = child.getFolderType();
			if (ft != null && cft != ft) {
				continue;
			}
			var jChild = jChildren;
			if (ft == null && cft != FolderType.NONE) {
				var o = map.get(cft);
				if (o == null) {
					map.put(cft, o = new JSONObject());
					jChildren.put(o);
					o.put("label", cft.displayName());
					o.put("icon", "folder");
					o.put("id", qname + ':' + cft.shortName());
					if (full) {
						o.put("children", jChild = new JSONArray());
					} else {
						o.put("children", true);
						continue;
					}
				} else if (full) {
					jChild = o.getJSONArray("children");
				} else {
					continue;
				}
			}
			var node = getNode(child, ft != null);
			jChild.put(node);
		}
		return jChildren;
	}
	
	private JSONArray getFileChildren(String id) throws Exception {
		var jChildren = new JSONArray();
		var split = id.split("/", 2);
		var project = (Project) Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(split[0]);
		var root = project.getDirFile();
		var file = new File(root, split[1]);
		if (file.isDirectory()) {
			var files = file.listFiles();
			Arrays.sort(files, (Comparator<File>) (f1, f2) -> {
				if (f1.isDirectory() == f2.isDirectory()) {
					return f1.getName().compareTo(f2.getName());
				}
				return f1.isDirectory() ? -1 : 1;
			});
			for (var f : files) {
				var o = new JSONObject();
				jChildren.put(o);
				o.put("label", f.getName());
				o.put("icon", f.isDirectory() ? "folder" : "file");
				o.put("id", id + "/" + f.getName());
				o.put("children", f.isDirectory());
			}
		}
		return jChildren;
	}
}
