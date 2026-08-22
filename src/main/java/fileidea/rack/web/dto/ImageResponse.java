package fileidea.rack.web.dto;

import fileidea.rack.common.ImageKind;
import fileidea.rack.common.TaskStatus;

public record ImageResponse(ImageKind kind, String url, TaskStatus status) {
}
