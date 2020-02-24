import {
  JsonController,
  Get,
  Post,
  Body,
  QueryParam,
  NotFoundError
} from "routing-controllers";

import { User } from "@/entity/User";

@JsonController("/api/v1/user")
export class UserController {
  @Get("/")
  async getById(@QueryParam("id") id: number) {
    const user = await User.findOne(id);
    if (!user) throw new NotFoundError();
    return user;
  }

  @Post("/")
  async create(@Body() user: User) {
    try {
      const newUser = User.create(user);
      return {
        status: "ok",
        user: newUser
      };
    } catch (e) {
      console.warn(e);
      return {
        status: "failed",
        request: {
          user
        }
      };
    }
  }
}
