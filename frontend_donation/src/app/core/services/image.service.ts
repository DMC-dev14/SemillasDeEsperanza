import { HttpClient } from "@angular/common/http";
import { Injectable, inject } from "@angular/core";
import { Observable } from "rxjs";
import { environment } from "../../../environments/environment";

@Injectable({ providedIn: 'root' })
export class ImageService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiBaseUrl}/api/images`;

  upload(file: File): Observable<{url: string, publicId: string}> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{url: string, publicId: string}>(this.apiUrl, formData);
  }
}