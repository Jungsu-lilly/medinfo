package com.medinfo.publicdata;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 공공데이터 XML 응답 파서. 외부 라이브러리 없이 JDK DOM만 사용.
 *
 * 응답은 두 가지 형태:
 *  1) 정상/업무 에러: response > header(resultCode, resultMsg) + body(items > item*, totalCount ...)
 *  2) 게이트웨이 에러: OpenAPI_ServiceResponse > cmmMsgHeader(errMsg, returnReasonCode)
 */
public final class PublicDataXmlParser {

    /** 잠시 후 재시도하면 풀릴 수 있는 게이트웨이 에러 코드 (01 내부오류, 04 HTTP, 05 타임아웃, 23 초당 한도) */
    private static final Set<String> RETRYABLE_CODES = Set.of("01", "04", "05", "23");

    private PublicDataXmlParser() {
    }

    public static PublicDataResponse parse(byte[] body) {
        Document doc = toDocument(body);
        Element root = doc.getDocumentElement();

        if ("OpenAPI_ServiceResponse".equals(root.getTagName())) {
            String code = text(root, "returnReasonCode");
            String msg = text(root, "errMsg");
            throw new PublicApiException(code, msg + " / " + text(root, "returnAuthMsg"), RETRYABLE_CODES.contains(code));
        }

        String resultCode = text(root, "resultCode");
        if (!"00".equals(resultCode)) {
            throw new PublicApiException(resultCode, text(root, "resultMsg"), RETRYABLE_CODES.contains(resultCode));
        }

        List<Map<String, String>> items = new ArrayList<>();
        NodeList itemNodes = root.getElementsByTagName("item");
        for (int i = 0; i < itemNodes.getLength(); i++) {
            Element item = (Element) itemNodes.item(i);
            Map<String, String> fields = new LinkedHashMap<>();
            NodeList children = item.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    fields.put(child.getNodeName(), child.getTextContent().trim());
                }
            }
            items.add(fields);
        }

        return new PublicDataResponse(
                items,
                intOrZero(text(root, "totalCount")),
                intOrZero(text(root, "pageNo")),
                intOrZero(text(root, "numOfRows"))
        );
    }

    private static Document toDocument(byte[] body) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 외부 엔티티(XXE) 차단: 외부에서 받은 XML이므로 기본으로 막아둔다
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(null); // 기본 핸들러의 stderr 출력 대신 아래 예외로만 알림
            return builder.parse(new ByteArrayInputStream(body));
        } catch (Exception e) {
            String head = new String(body, 0, Math.min(body.length, 200), java.nio.charset.StandardCharsets.UTF_8);
            throw new PublicApiException("PARSE", "XML 파싱 실패: " + head, false);
        }
    }

    /** root 아래 첫 번째 tag의 텍스트. 없으면 빈 문자열 */
    private static String text(Element root, String tag) {
        NodeList nodes = root.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }

    private static int intOrZero(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
